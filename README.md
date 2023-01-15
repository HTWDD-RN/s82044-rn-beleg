Tilman Ischner 
s82044
# Funktionalitäten:
- Server & Client funktionieren nach vorgegebenen Protokoll, jedoch nur SW
- Client zeigt Übertragungsfortschritt sowie Übertragungsrate an (wird mit jedem gesendeten Paket aktualisiert). 
- Client zeigt am Ende einer erfolgreichen Übertragung die Gesamtdauer und durchschn. Geschwindigkeit an.
- Server speichert Datei in seinem Ausführordner ab.
  - ggf. mit 1 am Ende des Namens falls Datei existiert

# Einschränkung
- nur SW-Protokoll implementiert
- Server kann nur einen Client händeln
- MTU & RTO statisch gesetzt

# Erfolgreich geprüft:  

- Funktion Ihres Clients + Server ohne Fehlersimulation
- Funktion Ihres Clients + Server mit Fehlersimulation
- Funktion Ihres Clients + Server über Hochschulproxy
- Funktion Ihres Clients + Hochschulserver ohne Fehlersimulation
- Funktion Ihres Clients + Hochschulserver mit Fehlersimulation