"""Minimal fake Home Assistant: frontend page with hassTokens, the WebSocket auth + supervisor/api calls, and an ingress page."""
import os
from fastapi import FastAPI, Request, WebSocket
from fastapi.responses import HTMLResponse, Response

app = FastAPI()
LOG = []

@app.get("/")
def root(expired: int = 0):
    # ?expired=1 stores an access token that has already run out (like after reopening the app), which the app must renew
    tok = "{access_token:'old',expires:Date.now()-1000" if expired else "{access_token:'tok',expires:Date.now()+1e6"
    return HTMLResponse("<html><body><h1>FAKE HA</h1><script>"
        "setTimeout(function(){localStorage.setItem('hassTokens', JSON.stringify(" + tok +
        ",refresh_token:'ref',clientId:location.origin+'/'}));},1500);"
        "</script></body></html>")

@app.post("/auth/token")
async def token(request: Request):
    form = dict(x.split("=", 1) for x in (await request.body()).decode().split("&"))
    LOG.append({"auth_token": form.get("grant_type")})
    if form.get("grant_type") != "refresh_token" or form.get("refresh_token") != "ref":
        return Response('{"error":"invalid_grant"}', status_code=400, media_type="application/json")
    return {"access_token": "tok", "expires_in": 1800, "token_type": "Bearer"}

# With MDA_UPSTREAM=http://127.0.0.1:8099 the ingress path forwards to a locally running add-on (real interface);
# without it a placeholder page is served.
UPSTREAM = os.environ.get("MDA_UPSTREAM", "").rstrip("/")

@app.api_route("/api/hassio_ingress/TOKEN123/{path:path}", methods=["GET", "POST", "PUT", "PATCH", "DELETE"])
async def ingress(path: str, request: Request):
    if not UPSTREAM:
        return HTMLResponse("<html><body><h1>MDA INGRESS OK</h1></body></html>")
    import httpx
    headers = {k: v for k, v in request.headers.items() if k.lower() in ("content-type", "accept", "accept-language")}
    async with httpx.AsyncClient() as c:
        r = await c.request(request.method, f"{UPSTREAM}/{path}", params=request.query_params, content=await request.body(), headers=headers)
    keep = {k: v for k, v in r.headers.items() if k.lower() in ("content-type", "cache-control", "content-disposition")}
    return Response(r.content, status_code=r.status_code, headers=keep)

@app.get("/log")
def log():
    return LOG

@app.websocket("/api/websocket")
async def ws(w: WebSocket):
    await w.accept()
    await w.send_json({"type": "auth_required"})
    m = await w.receive_json()
    if m.get("access_token") != "tok":
        LOG.append({"auth_invalid": m.get("access_token")})   # real HA: "Login attempt failed" notification
        await w.send_json({"type": "auth_invalid"}); return
    await w.send_json({"type": "auth_ok"})
    while True:
        m = await w.receive_json()
        LOG.append(m)
        if m["endpoint"] == "/ingress/session":
            await w.send_json({"id": m["id"], "type": "result", "success": True, "result": {"session": "SESS"}})
        else:
            await w.send_json({"id": m["id"], "type": "result", "success": True, "result": {"ingress_url": "/api/hassio_ingress/TOKEN123/"}})
