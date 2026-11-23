from pathlib import Path
from lxml import etree as E
import zipfile,json
src=Path('C:/Users/tania/Documents/Presentacion_Sol_de_Shira_Sprint_1.pptx');base=Path('outputs/presentacion_revision');ns={'a':'http://schemas.openxmlformats.org/drawingml/2006/main','p':'http://schemas.openxmlformats.org/presentationml/2006/main'}
# Text nodes only. Positions, fonts, paragraphs, images, themes and layouts remain byte-for-byte unchanged outside edited XML parts.
replacements={
2:{'[n.º]':'Sol de Shirá','[Grado. Nombres y apellidos]':'Por confirmar'},
3:{'Problema de negocio y trabajos relacionados':'Problema de negocio y antecedentes','   ·   2 min':'   ·   3 min','   ·   3 min':'   ·   6 min','   ·   4 min':'   ·   5 min','10 min de exposición + 5 min de preguntas':'15 min de exposición. Preguntas al finalizar'},
4:{'Problema: la gestión fragmentada de reservas y pagos dificulta mantener una disponibilidad compartida (demoras, duplicidad, cruce de fechas).':'Problema: la gestión fragmentada dificulta compartir la disponibilidad y genera riesgo de demoras y cruces de reservas.','Brecha: la literatura [4]–[7] trata calidad y adaptación de Scrum, sin medir tiempos ni errores en un ciclo de hospedaje.':'Antecedentes: los estudios [4]–[7] orientan el análisis de Scrum. El impacto en Sol de Shirá requiere mediciones propias.'},
5:{'los estudios se centran en calidad y adaptación de Scrum, sin medir tiempos ni errores en reservas. Nuestro caso aporta un ciclo de hospedaje trazable (adelanto y check-in) con límites explícitos.':'los estudios orientan la revisión de calidad y adaptación de Scrum. Nuestro caso describe el ciclo de hospedaje; su impacto requiere mediciones del negocio.'},
6:{'10 RF y 4 RNF del Sprint 1 [1].':'10 RF y 4 RNF documentados para el Sprint 1.','RF → HU → prueba (p. ej., RF10 → HU7 → CheckInTest).':'RF → HU → prueba (RF10 → HU7 → CheckInTest y PagoRestanteTest).','registrar motivo, historia afectada, decisión y prueba antes de actualizar estados (propuesto; sin cambios registrados).':'registrar motivo, HU, decisión y prueba (propuesta; registro de cambios no verificado).','descripción del negocio del equipo; video de entrevista pendiente.':'descripción del negocio e historias de usuario; video de entrevista por revisar.'},
7:{'Permite entregar incrementos y revisar el alcance con el negocio [2]. No hay evaluación experimental frente a Kanban o XP [8].':'Permite inspeccionar incrementos [2]. La evaluación propuesta usa un instrumento propio con escala 2–1–0 según evidencia.','1 analizado (31/10–04/11); duración por confirmar':'Sprint 1. Tablero: 31/10–04/11/2026; duración pactada por confirmar','cortes del tablero (31/10–04/11) y bitácora; Daily, Review y Retrospective por confirmar':'tablero y bitácora documentan avance. Acuerdos y cumplimiento de los eventos requieren verificación.','Product Backlog priorizado · Sprint Backlog · HU con criterios de aceptación · Definición de Terminado · tablero en Google Sheets (Jira no verificado)':'Product Backlog, Sprint Backlog e Incremento. Apoyo: HU, criterios de aceptación y tablero en Google Sheets. La DoD define la calidad exigida al Incremento.'},
8:{'JWT y BCrypt para acceso seguro; REST y JPA; Mercado Pago para el adelanto con notificaciones verificadas.':'JWT para autenticación y BCrypt para contraseñas; REST, JPA y Mercado Pago para adelanto y validación de notificaciones.','5 casos (CP01–CP05) trazados a HU4–HU7; ejecución por reproducir.':'5 escenarios representativos (CP01–CP05) trazados a HU4–HU7; ejecución del corte por reproducir.'},
10:{'5 casos':'5 escenarios','De prueba localizados; ejecución por reproducir':'Representativos; ejecución histórica por verificar','el burndown baja de 43 unidades a 0, pero la suma nominal es 42 pts y la serie planificada termina en −1; no se calcula velocidad hasta reconciliar. Las pruebas existen, pero falta ejecutarlas.':'El burndown registra 43 unidades y termina en 0; la selección suma 42 puntos y la serie planificada llega a −1. Falta conciliar las medidas y verificar la ejecución histórica.','Incremento entregado':'Alcance documentado','Pts. compr.':'Pts. selecc.','Pts. compl.':'Pts. acept.'},
11:{'Inicio de sesión con correo y contraseña; rol según usuario (HU21)':'Autenticación por correo y contraseña (HU21 en selección).','Panel y detalle de reserva; saldo restante y check-in registrados (HU5–HU7)':'Consultar panel y detalle; registrar saldo y check-in en versión de prueba (HU5–HU7)','Evidencia del avance por sprint':'Evolución registrada del Sprint 1; unidades por conciliar','capturas de respaldo del flujo reserva → adelanto → check-in y del tablero, listas si falla la conexión.':'preparar capturas del flujo reserva → adelanto → check-in y del tablero como respaldo.'},
14:{'¿Preguntas?':'¿Preguntas al finalizar?','   ·   5 min':''}}
with zipfile.ZipFile(src) as z:files={k:z.read(k) for k in z.namelist()}
log=[]
for number,changes in replacements.items():
 key=f'ppt/slides/slide{number}.xml';root=E.fromstring(files[key]);found=set()
 for t in root.findall('.//a:t',ns):
  if t.text in changes:
   old=t.text;t.text=changes[old];found.add(old);log.append({'slide':number,'before':old,'after':t.text})
 missing=set(changes)-found
 if missing:print('NOT FOUND',number,missing)
 if number==2:
  initials={1000:'CG',1001:'JP',1002:'MA',3007:'AB',3003:'LO'}
  for sp in root.findall('.//p:sp',ns):
   sid=int(sp.find('p:nvSpPr/p:cNvPr',ns).get('id'));ts=sp.findall('.//a:t',ns)
   if sid in initials and len(ts)==1:ts[0].text=initials[sid]
 files[key]=E.tostring(root,xml_declaration=True,encoding='UTF-8',standalone=True)
# Correct any unsupported statements duplicated in the notes, without changing note structure.
for key,data in list(files.items()):
 if key.startswith('ppt/notesSlides/notesSlide') and key.endswith('.xml'):
  root=E.fromstring(data);changed=False
  for t in root.findall('.//a:t',ns):
   if t.text:
    old=t.text
    for pairs in replacements.values():
     if t.text in pairs:t.text=pairs[t.text]
    changed|=old!=t.text
  if changed:files[key]=E.tostring(root,xml_declaration=True,encoding='UTF-8',standalone=True)
out=base/'build/candidate.pptx'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for k,v in files.items():z.writestr(k,v)
# Verify that every non-text XML element and attribute is identical, including all design geometry and font attributes.
with zipfile.ZipFile(src) as z:
 def no_text(data):
  r=E.fromstring(data)
  for t in r.findall('.//a:t',ns):t.text=''
  return E.tostring(r,method='c14n')
 for k,v in files.items():
  if v!=z.read(k):assert no_text(v)==no_text(z.read(k)),k
print('Verified: text-only changes; all design elements preserved.')
(base/'build/changes.json').write_text(json.dumps(log,ensure_ascii=False,indent=2),encoding='utf-8')
print(out)
