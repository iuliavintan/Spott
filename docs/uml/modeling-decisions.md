# Diagrame și reguli confirmate pentru Spott

Modelele acoperă toate funcționalitățile proiectului, inclusiv detecția asistată, geofencing, alertele, heatmap-ul și indicatorul de încredere. Fotografiile sunt excluse din domeniul curent.

Regulile de mai jos sunt reperul pentru diagramele curente. README-urile au fost păstrate la cerere.

## Diagrame

| Tip | Sursă | Previzualizare | Perspectivă |
| --- | --- | --- | --- |
| Use-case, actualizată | [use-cases.puml](use-cases.puml) | [SVG](use-cases.svg) | Roluri și obiective, inclusiv anularea plecării. |
| Activitate | [parking-activity.puml](parking-activity.puml) | [SVG](parking-activity.svg) | Salvarea parcării și evoluția unui raport, cu decizii și excepții. |
| Secvență | [parking-sequence.puml](parking-sequence.puml) | [SVG](parking-sequence.svg) | Căutare într-o zonă fixă, raportare, alerte și verificarea proximității. |
| Clase | [domain-classes.puml](domain-classes.puml) | [SVG](domain-classes.svg) | Entități, valori, responsabilități și multiplicități. |
| Flux de date, nivelul 1 | [data-flow.dot](data-flow.dot) | [SVG](data-flow.svg) | Procese, entități externe, stocuri și circulația datelor. |

Cele patru tipuri noi corespund materialului **Requirements engineering.pdf**: secvență la p. 13, activitate la p. 14, clase la p. 15 și flux de date la p. 18. Împreună acoperă perspectivele comportamentală, structurală și funcțională de la p. 9. DFD-ul folosește notație cu procese ovale și stocuri între linii paralele; sursa sa este Graphviz DOT.

DFD-ul este grupat în patru secțiuni: cont și locația mașinii, rapoarte, descoperire și alerte, respectiv importul datelor de referință. Simbolurile cu același ID reprezintă aceeași entitate sau același stoc; repetarea lor evită săgețile care traversează alte secțiuni. `D1` grupează datele private ale șoferului, `D2` rapoartele și feedbackul, `D3` datele de referință, iar `D4` cercurile de căutare și preferințele aferente. Etichetele scurte descriu categorii de date; regulile detaliate sunt păstrate mai jos.

## Reguli funcționale

1. **Confirmare obligatorie.** Semnalele de activitate, localizare și revenire la mașină pot genera sugestii. Șoferul confirmă salvarea parcării și plecarea; niciun semnal nu confirmă automat aceste acțiuni. Salvarea și raportarea manuală rămân disponibile.
2. **Locația personală și raportul public sunt distincte.** Salvarea poziției mașinii nu publică un loc de parcare. La raportare, șoferul alege poziția salvată sau o poziție manuală. Modelul păstrează o singură poziție curentă a mașinii per cont, conform funcției de regăsire a ultimei parcări.
3. **Anulare.** Autorul poate anula un anunț `POTENTIALLY_LEAVING` încă valabil. Anularea nu este o modalitate de a redeschide un raport deja închis.
4. **Expirare.** Un raport de plecare anticipată are un termen inițial. Confirmarea explicită a plecării îl transformă în `RECENTLY_VACATED` și pornește intervalul de valabilitate al acestei stări. Momentele și expirarea sunt calculate cu timpul serverului.
5. **„Ocupat” numai din apropiere.** Un șofer autentificat poate închide imediat un raport activ numai dacă backendul acceptă o poziție recentă, suficient de precisă și apropiată de poziția raportată. O locație aleasă manual pe hartă nu este dovadă de proximitate. Lipsa permisiunii de localizare, datele vechi, precizia insuficientă și distanța prea mare duc la respingerea feedbackului.
6. **Feedback și concurență.** `AVAILABLE` se atașează numai unui raport `RECENTLY_VACATED` încă valabil și nu prelungește `expiresAt`. Tranzițiile verifică atomic autorul unde este necesar, starea curentă, termenul și dovezile aplicabile. Rapoartele închise nu se redeschid prin feedback întârziat.
7. **Căutare activă într-un cerc fix.** Șoferul alege centrul și raza, exprimată în metri. Deplasarea telefonului nu mută centrul. Doar o acțiune explicită a utilizatorului poate schimba zona. Un raport se califică dacă poziția sa se află în cercul salvat și corespunde filtrelor.
8. **Alerte.** Se generează pentru căutări active, rapoarte încă valabile și preferințe/permisiuni compatibile. Oprirea căutării oprește generarea unor alerte noi. Mesajele deja trimise pot ajunge ulterior; la deschidere se citește starea actuală a raportului.
9. **Hartă și navigație.** Harta oferă status, vechime, filtre și detalii. Localizarea curentă poate ajuta căutarea obișnuită; selectarea manuală a unei zone rămâne posibilă. Navigația primește destinația selectată. Expirarea este verificată la citire, chiar dacă procesarea de fundal nu a actualizat încă starea la `EXPIRED`.
10. **Date agregate și de referință.** Heatmap-ul folosește suficiente observații relevante; un anunț anulat nu este tratat drept dovadă că locul s-a eliberat. Indicatorul de încredere este euristic. Zonele, regulile și categoriile importate se folosesc împreună cu proveniența și verificarea lor; existența unei zone de parcare nu indică disponibilitatea unui loc.

## Tranzițiile raportului

| Din | În | Condiție |
| --- | --- | --- |
| Raport nou | `POTENTIALLY_LEAVING` | Șoferul confirmă publicarea anunțului. |
| `POTENTIALLY_LEAVING` | `RECENTLY_VACATED` | Autorul confirmă explicit plecarea înainte de expirare. |
| `POTENTIALLY_LEAVING` | `CANCELLED` | Autorul anulează anunțul înainte de expirare. |
| Oricare stare activă | `OCCUPIED` | Feedback „ocupat” acceptat după verificarea proximității. |
| Oricare stare activă | `EXPIRED` | Timpul serverului ajunge la `expiresAt`. |

Stările active sunt `POTENTIALLY_LEAVING` și `RECENTLY_VACATED`. Acestea descriu rapoarte ale comunității, nu rezervări sau garanții de disponibilitate.

## Parametri care rămân de stabilit numeric

| Parametru | Rol |
| --- | --- |
| `upcomingTtl` | Valabilitatea unui anunț de plecare anticipată. |
| `vacatedTtl` | Valabilitatea unui raport după confirmarea plecării. |
| `occupiedProximityMeters` | Distanța maximă acceptată pentru feedback „ocupat”. |
| `maxLocationAge` | Vechimea maximă a poziției folosite ca dovadă. |
| `maxLocationAccuracyMeters` | Limita acceptată a incertitudinii poziției. |

Diagrama de clase grupează aceste valori în `ReportPolicy`. Raza unei căutări este aleasă de utilizator și este distinctă de pragul pentru feedback. Valorile concrete și limitele permise ale razei vor fi stabilite la implementare.

## Randare și verificare

Sursele sunt independente. Diagramele UML se randează local cu PlantUML, iar DFD-ul cu Graphviz:

```sh
java -jar path/to/plantuml.jar -tsvg "docs/uml/*.puml"
dot -Tsvg docs/uml/data-flow.dot -o docs/uml/data-flow.svg
```

Previzualizările sunt generate cu PlantUML 1.2026.9 și Graphviz 2.44.1. La verificarea use-case-ului, prefixul englezesc `UC_` și pachetele de tip folder sunt convenții păstrate în proiect; sugestiile validatorului despre `CU_` și pachete dreptunghiulare sunt stilistice.

Referințe de sintaxă: [activitate](https://plantuml.com/activity-diagram-beta), [secvență](https://plantuml.com/sequence-diagram), [clase](https://plantuml.com/class-diagram) și [Graphviz DOT](https://graphviz.org/doc/info/lang.html). Pentru use-case se aplică checklist-ul skill-ului local `.agents/skills/uml-use-case-diagram/`.
