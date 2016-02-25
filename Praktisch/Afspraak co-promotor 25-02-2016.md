#Meeting met Peter Leemans & Jan Bogaerts
Eerstvolgende afspraak: 10/03/2016, kijken hoe ver ik sta met implementeren van de gateway.

Principe van de proof of concept uitleggen:

Momenteel hebben Bluetooth Low Energy accessoires meestal een closed system. De devices gebruiken veelal geen standaard SIG-Adopted BLE services, dus is het ontdekken van de functie ervan redelijk moeilijk. Indien er daar bovenop nog eens authenticatie en encryptie op zit, is het bijna onmogelijk om dit te reverse engineeren.

Bij sommige devices is er synchronisatie met de Cloud beschikbaar, maar enkel en alleen een cloud portaal van de developer. Dit geeft geen volledig zicht over alle ruwe data die uit de sensoren is gekomen. Doel is om in te toekomst een open platform te hebben waar er automatische discovery van servcies kan gebeuren, automatisch de devices en sensoren registreren op de ATT cloud en te synchroniseren.

* Bluetooth Low Energy gateway bouwen op mobile (of desktop, er is nog geen Java port voor hun libraries)
    * Gateway bestaat uit BLE SDK (dus native libraries), de ATT SDK (communicatie met ATT cloud) en een layer die de data uit de BLE SDK haalt en naar de ATT SDK stuurt
    * Taak is om die layer te bouwen
    * In eerste instantie proberen connecteren met eigen device (Arduino met Heart Rate Service)
    * Later connecteren met bestaande producten (Flower Power, V.BTTN, Misfit Shine 2)

Ook kort eens API overlopen van ATT. Code samples gekregen over hoe te gebruiken etc.

Volgende afspraak: op de Android app de heart rate proberen lezen, nog niet doorsturen