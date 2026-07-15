# Documentatie afname (Linked Open Data)


Deze documentatie beschrijft het RIE-IEPR-datamodel vanuit het perspectief van een data-afnemer die de gegevens raadpleegt via Linked Open Data (LOD). De documentatie behandelt **wat er beschikbaar is** en **hoe u het kunt gebruiken**. Er wordt geen informatie gegeven over databanken, transformatieprocessen of applicatielogica.

## Inhoud

- [Gebruiksscenario's](./GEBRUIKSSCENARIO.md) — concrete voorbeelden van data-afname met SPARQL-query's
- [Basisaannames](./BASISAANNAME.md) — de modellen en aannames die ten grondslag liggen aan het datamodel
- [Exploitant- en exploitatiemodel](./EXPLOITANT.md) — organisaties, locaties en activiteiten
- [Systemen: installaties, emissiepunten en meetpunten](./SYSTEMEN.md) — systemen, subsystemen en eigenschappen
- [Observaties en emissies](./OBSERVATIES.md) — metingen, observaties en gebeurtenissen (emissie, onttrekking, uitwisseling)
- [Aangifte en dossier](./AANGIFTE.md) — documenten en transacties gekoppeld aan de data
- [Versiebeheer en tijdsrecht](./VERSIEBEHEER.md) — versiebeheer, geldigheid en historische query's

> **Codelijsten**: Een overzicht van alle gecontroleerde vocabulaires (SKOS-concepten) die in dit model worden gebruikt, vindt u hieronder onder "Codelijsten (SKOS-concepten)". Deze codelijsten worden beheerd in het aparte repository [milieuinfo/codelijst-rie-iepr](https://github.com/milieuinfo/codelijst-rie-iepr/).

## Hoe deze documentatie gebruiken

Elk bestand bevat **concrete datavoorbeelden** uit de RIE-IEPR-ontologie, gebaseerd op realistische data van AGC Glass Europe. De Turtle-snippets tonen hoe de data eruitziet in RDF-formaat — direct bruikbaar voor LOD-afnemers die SPARQL of RDF-libraries gebruiken.

### URI-patronen

De meeste entiteiten volgen een consistent URI-patroon:

| Entiteitstype | URI-patroon | Voorbeeld |
|---|---|---|
| Exploitant (identity) | `.../exploitant/{uuid}` | `.../exploitant/019e9271-1452-7630-be04-59ea199007a7` |
| Exploitatie (versie) | `.../exploitatie/{uuid}/{issued}/{created}` | `.../exploitatie/019e9271-1454-7b38-9eae-505cace7ca54/2026-01-01/2026-01-01T10:00:00Z` |
| Installatie (versie) | `.../installatie/{uuid}/{issued}/{created}` | `.../installatie/019e9271-1456-7a2f-ac4e-8904bab88f37/2026-01-01/2026-01-01T10:00:00Z` |
| Emissiepunt (versie) | `.../emissiepunt/{uuid}/{issued}/{created}` | `.../emissiepunt/019e9271-145b-75f5-83d9-fe9b0b7e9540/2026-01-01/2026-01-01T10:00:00Z` |
| Observatie | `.../observatie/{uuid}/{created}` | `.../observatie/019edc4a-1a35-7bn3-im4f-n9ojk7kgkf4/2026-01-01T10:00:00Z` |

Versieerbare entiteiten bevatten **drie** segmenten in de URI: een unieke identifier, de geldigheidsdatum (`issued`) en de aanmaaktimestamp (`created`). De tijdsloze identity-uri (twee segmenten) is bereikbaar via `dct:isVersionOf`.

## Externe standaarden

Het RIE-IEPR-datamodel bouwt voort op volgende W3C-standaarden:

- **SOSA/SSN** ([Sensor Web Observation Model](https://www.w3.org/TR/vocab-ssn/)) — systemen, observaties, metingen
- **PROV-O** ([Provenance Ontology](https://www.w3.org/TR/prov-o/)) — herkomst, versiebeheer
- **GeoSPARQL** ([OGC GeoSPARQL](https://docs.ogc.org/is/22-047r1/22-047r1.html)) — geospatiale objecten
- **P-Plan** ([Plan Ontology](https://www.opmw.org/model/p-plan/)) — processen en stappen

Zie ook de [gegenereerde specificatie](../bin/specificatie/) voor een volledig overzicht van klassen, eigenschappen en concepten.

## Codelijsten (SKOS concepten)

Het RIE-IEPR-datamodel maakt uitgebreid gebruik van **gecontroleerde vocabulaires** (codelijsten), voorgesteld als [SKOS concepten](https://www.w3.org/TR/skos-reference/). Deze codelijsten worden beheerd in een apart repository: **[milieuinfo/codelijst-rie-iepr](https://github.com/milieuinfo/codelijst-rie-iepr/)**.

De codelijsten zijn gepubliceerd als Linked Data op [data.omgeving.vlaanderen.be](https://data.omgeving.vlaanderen.be/id/concept/riepr/) en worden in de ontologie verwezen via hun volledige URI. Ze vormen de waarden voor `dct:type`, `adms:status` en andere categorisatie-eigenschappen.

### Beschikbare codelijsten

| Codelijst | URI-prefix | Beschrijving |
|---|---|---|
| **installatie_type** | `…/concept/riepr/installatie-type/` | Typen installaties (GPBV, IEPR, stookinstallatie, …) |
| **emissiepunt_type** | `…/concept/riepr/emissiepunt-type/` | Typen emissiepunten (schoorsteen, lozingspunt, …) |
| **onttrekkingspunt_type** | `…/concept/riepr/onttrekkingspunt-type/` | Typen onttrekkingspunten (grondwaterput, …) |
| **meetpunt_type** | `…/concept/riepr/meetpunt-type/` | Typen meetpunten (meetput, controle-inrichting, …) |
| **meetinstrument_type** | `…/concept/riepr/meetinstrument-type/` | Typen meetinstrumenten (debietmeter, …) |
| **filter_type** | `…/concept/riepr/filter-type/` | Typen filters |
| **uitwisselpunt_type** | `…/concept/riepr/uitwisselpunt-type/` | Typen uitwisselpunten |
| **procedure_type** | `…/concept/riepr/procedure-type/` | Procesprocedures (emissie, onttrekking, verwerking, meet, uitwissel) |
| **hoofdactiviteit_type** | `…/concept/riepr/hoofdactiviteit-type/` | Hoofdactiviteit van een exploitant |
| **status_type** | `…/concept/riepr/status-type/` | Statussen (in_dienst, ontmanteld, …) |
| **rubriek_type** | `…/concept/riepr/rubriek-type/` | Classificaties (VLAREM, EGW, …) |
| **aangifte_type** | `…/concept/riepr/aangifte-type/` | Typen aangiften |
| **data_type** | `…/concept/riepr/data-type/` | Datatype-kinds |
| **emissiepunt_eigenschappen** | `…/concept/riepr/emissiepunt-eigenschappen/` | Koppeling emissiepunt-types en hun eigenschappen |
| **installatie_eigenschappen** | `…/concept/riepr/installatie-eigenschappen/` | Koppeling installatie-types en hun eigenschappen |
| **filter_eigenschappen** | `…/concept/riepr/filter-eigenschappen/` | Koppeling filter-types en hun eigenschappen |

### Voorbeeld: procedure_type in gebruik

```turtle
@prefix dct: <http://purl.org/dc/terms/> .

# Een emissieproces heeft een procedure_type als dct:type
<.../proces/019eaca0-b8c6-7240-ac66-b7831d1b3623/2026-01-01/2026-01-01T10:00:00Z>
    dct:type <https://data.omgeving.vlaanderen.be/id/concept/riepr/procedure-type/emissie> .

# Dit dwingt het proces om een emissiepunt te implementeren (OWL-axioma)
<.../proces/019eaca0-b8c6-7240-ac66-b7831d1b3623/2026-01-01/2026-01-01T10:00:00Z>
    ssn:implementedBy <.../emissiepunt/019eaca0-b8c6-7096-886c-103c3e21466c/2026-01-01/2026-01-01T10:00:00Z> .
```

### Voorbeeld: status_type in gebruik

```turtle
@prefix adms: <http://www.w3.org/ns/adms#> .

<.../installatie/019e9271-1456-7a2f-ac4e-8904bab88f37/2026-01-01/2026-01-01T10:00:00Z>
    adms:status <https://data.omgeving.vlaanderen.be/id/concept/riepr/status-type/in_dienst> .
```

### Codelijsten als LOD

De codelijsten zelf zijn gepubliceerd als Linked Open Data en kunnen worden geraadpleegd via SPARQL of directe URI-toegang:

- **Turtle**: `https://data.omgeving.vlaanderen.be/id/concept/riepr/installatie-type.ttl`
- **JSON-LD**: `https://data.omgeving.vlaanderen.be/id/concept/riepr/installatie-type.jsonld`
- **Individual concept**: `https://data.omgeving.vlaanderen.be/id/concept/riepr/installatie-type/gpbv-installatie`

Het codelijsten-repository wordt gegenereerd uit CSV-bronbestanden en gepubliceerd in meerdere formaten (Turtle, JSON-LD, N-Triples, JSON, CSV, Parquet, Excel).

## Databronnen

Deze documentatie is gebaseerd op:

- **Ontologie**: `documentatie/datamodel/src/ns/riepr/riepr.ttl`
- **Datavoorbeeld**: `documentatie/datamodel/datavoorbeelden/agc-glass_MJV_01-07-2026.ttl` (AGC Glass Europe)
- **SHACL-shapes**: `documentatie/datamodel/generated/shacl/schema.ttl`