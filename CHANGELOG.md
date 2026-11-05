# Changelog

Änderungen an der Android-App. Die HA-App hat ihren eigenen [Changelog](https://github.com/Schumey/MyDietAssistant/blob/main/mydiet/CHANGELOG.md).

## 0.4.1

### Geändert
- Update-Dialog zeigt die Release-Notizen als Text ohne Markdown-Zeichen (nur den deutschen Teil).
- Nach dem einmaligen „Unbekannte Apps installieren“ startet das Update beim Zurückkehren von selbst, statt dass man erneut auf Installieren tippen muss.

## 0.4.0

### Neu
- **Eigenes Repository:** Die App liegt jetzt in `Schumey/MyDietAssistant-Android` (vorher Ordner `android/` im Haupt-Repo, Verlauf übernommen).
- **Update-Prüfung:** Beim Start höchstens einmal am Tag sucht die App unter den Releases dieses Repos nach einer neueren Version und bietet sie an (**Installieren**, **Später**, **Überspringen**). Sie lädt die APK selbst und startet die Installation. Von Hand: *Einstellungen → Android-App → Nach App-Updates suchen* (braucht MyDietAssistant 1.30.0).

## 0.3.0

### Neu
- **Health Connect:** Gewicht, Schritte und aktive Kalorien lesen und an MyDietAssistant übergeben (Knopf und automatische Übernahme in *Einstellungen → Android-App*), mit Datenschutz-Hinweis für den Berechtigungsdialog.
- **Bericht:** „Bericht öffnen“ öffnet den Bericht in der App, „Drucken / als PDF speichern“ den Druckdialog von Android.

## 0.2.0

### Neu
- **Barcode-Scanner des Systems** (Google Code Scanner, ohne Kamera-Berechtigung).
- **Widget** mit den Kalorien von heute und den Knöpfen „Scannen“ und „Eintragen“.

## 0.1.0

### Neu
- Hülle mit HA-Anmeldung im WebView, Ingress-Sitzung, Vollbild; Kamera, Datei-Auswahl und Downloads.
