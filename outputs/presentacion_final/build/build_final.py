from pathlib import Path
from lxml import etree as E
from PIL import Image
import zipfile,copy,json
B=Path('outputs/presentacion_final');N={'a':'http://schemas.openxmlformats.org/drawingml/2006/main','p':'http://schemas.openxmlformats.org/presentationml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'};RN='http://schemas.openxmlformats.org/package/2006/relationships';CN='http://schemas.openxmlformats.org/package/2006/content-types'
SRC='C:/Users/tania/Downloads/Articulo_Sol_de_Shira_Sprint1.pptx'
with zipfile.ZipFile(SRC) as z:F={k:z.read(k) for k in z.namelist()}
def q(k):a,b=k.split(':');return '{'+N[a]+'}'+b
def read(k):return E.fromstring(F[k])
def save(k,r):F[k]=E.tostring(r,xml_declaration=True,encoding='UTF-8',standalone=True)
def fill(tx,text,size=None):
 ps=tx.findall('a:p',N);pr=ps[0].find('a:pPr',N) if ps else None;rp=tx.find('.//a:rPr',N);rp=copy.deepcopy(rp) if rp is not None else E.Element(q('a:rPr'))
 if size:rp.set('sz',str(size))
 for p in ps:tx.remove(p)
 for line in text.split('\n'):
  p=E.SubElement(tx,q('a:p'))
  if pr is not None:p.append(copy.deepcopy(pr))
  r=E.SubElement(p,q('a:r'));r.append(copy.deepcopy(rp));E.SubElement(r,q('a:t')).text=line

def shape(r,sid):return next(s for s in r.findall('.//p:sp',N) if s.find('p:nvSpPr/p:cNvPr',N).get('id')==str(sid))
def edit(i,m,size=None):
 k=f'ppt/slides/slide{i}.xml';r=read(k)
 for sid,txt in m.items():fill(shape(r,sid).find('p:txBody',N),txt,size)
 save(k,r)
def geom(sp,x,y,w,h):
 xf=sp.find('p:spPr/a:xfrm',N)
 if xf is None:xf=sp.find('p:xfrm',N)
 off=xf.find('a:off',N);ex=xf.find('a:ext',N);off.set('x',str(x));off.set('y',str(y));ex.set('cx',str(w));ex.set('cy',str(h))
def clone(src,num):
 F[f'ppt/slides/slide{num}.xml']=F[f'ppt/slides/slide{src}.xml'];rel=read(f'ppt/slides/_rels/slide{src}.xml.rels')
 for v in list(rel):
  if v.get('Type','').endswith('/notesSlide'):v.set('Target',f'../notesSlides/notesSlide{num}.xml')
 save(f'ppt/slides/_rels/slide{num}.xml.rels',rel)
 F[f'ppt/notesSlides/notesSlide{num}.xml']=F[f'ppt/notesSlides/notesSlide{src}.xml'];nr=read(f'ppt/notesSlides/_rels/notesSlide{src}.xml.rels')
 for v in nr:
  if v.get('Type','').endswith('/slide'):v.set('Target',f'../slides/slide{num}.xml')
 save(f'ppt/notesSlides/_rels/notesSlide{num}.xml.rels',nr)
 ct=read('[Content_Types].xml')
 for part,kind in [(f'/ppt/slides/slide{num}.xml','slide'),(f'/ppt/notesSlides/notesSlide{num}.xml','notesSlide')]:E.SubElement(ct,'{'+CN+'}Override',PartName=part,ContentType=f'application/vnd.openxmlformats-officedocument.presentationml.{kind}+xml')
 save('[Content_Types].xml',ct)

def image(i,path,x,y,w,h,name):
 r=read(f'ppt/slides/slide{i}.xml');rel=read(f'ppt/slides/_rels/slide{i}.xml.rels');rid='rIdEvidence'+str(i);target=f'final-evidence-{i}.png';E.SubElement(rel,'{'+RN+'}Relationship',Id=rid,Type=N['r']+'/image',Target='../media/'+target);save(f'ppt/slides/_rels/slide{i}.xml.rels',rel);F['ppt/media/'+target]=Path(path).read_bytes()
 iw,ih=Image.open(path).size;scale=min(w/iw,h/ih);ww,hh=int(iw*scale),int(ih*scale)
 p=E.fromstring(f'''<p:pic xmlns:p="{N['p']}" xmlns:a="{N['a']}" xmlns:r="{N['r']}"><p:nvPicPr><p:cNvPr id="2500" name="{name}"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="{x+(w-ww)//2}" y="{y+(h-hh)//2}"/><a:ext cx="{ww}" cy="{hh}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic>''');r.find('p:cSld/p:spTree',N).append(p);save(f'ppt/slides/slide{i}.xml',r)
 ct=read('[Content_Types].xml')
 if not any(v.get('Extension')=='png' for v in ct):E.SubElement(ct,'{'+CN+'}Default',Extension='png',ContentType='image/png')
 save('[Content_Types].xml',ct)

def native(r,sid,text,x,y,w,h,size=2200,box=True):
 p=E.fromstring(f'''<p:sp xmlns:p="{N['p']}" xmlns:a="{N['a']}"><p:nvSpPr><p:cNvPr id="{sid}" name="Diagrama editable {sid}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill><a:ln w="18000"><a:solidFill><a:srgbClr val="043B61"/></a:solidFill></a:ln></p:spPr><p:txBody><a:bodyPr wrap="square" anchor="ctr" lIns="90000" rIns="90000" tIns="60000" bIns="60000"/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="es-PE" sz="{size}"><a:solidFill><a:srgbClr val="043B61"/></a:solidFill><a:latin typeface="Arial"/></a:rPr><a:t/></a:r></a:p></p:txBody></p:sp>''')
 if not box:
  pr=p.find('p:spPr',N);pr.remove(pr.find('a:solidFill',N));E.SubElement(pr,q('a:noFill'));pr.remove(pr.find('a:ln',N))
 fill(p.find('p:txBody',N),text,size);r.find('p:cSld/p:spTree',N).append(p);return p

def line(r,sid,x1,y1,x2,y2):
 x,y=min(x1,x2),min(y1,y2);w,h=abs(x2-x1),abs(y2-y1)
 p=E.fromstring(f'''<p:cxnSp xmlns:p="{N['p']}" xmlns:a="{N['a']}"><p:nvCxnSpPr><p:cNvPr id="{sid}" name="Referencia del diagrama"/><p:cNvCxnSpPr/><p:nvPr/></p:nvCxnSpPr><p:spPr><a:xfrm flipH="{'1' if x2<x1 else '0'}" flipV="{'1' if y2<y1 else '0'}"><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm><a:prstGeom prst="line"><a:avLst/></a:prstGeom><a:ln w="16000"><a:solidFill><a:srgbClr val="567780"/></a:solidFill><a:tailEnd type="triangle"/></a:ln></p:spPr></p:cxnSp>'''.replace('y2<y1','y2<y1'))
 r.find('p:cSld/p:spTree',N).append(p)

# Verified, preliminary scoring of the existing 32 conditions.
scores={'PB1':2,'PB2':2,'PB3':0,'PB4':1,'PB5':1,'SP1':1,'SP2':2,'SB1':2,'SB2':2,'EJ1':2,'EJ2':1,'EJ3':1,'EJ4':0,'EJ5':0,**{f'DS{i}':0 for i in range(1,7)},'RV1':0,'RV2':0,'RV3':0,'RV4':0,'RT1':0,'RT2':0,'RT3':0,'RT4':0,'IN1':2,'IN2':0,'IN3':1,'IN4':0}
assert len(scores)==32 and sum(scores.values())==20
reasons={
'PB1':'Sprints!D3:E10 y E24:E31 vinculan necesidades del hospedaje con HU.',
'PB2':'Sprints!E24:E31 describe actor, necesidad y resultado.',
'PB3':'Hay selección y estimaciones; no se acredita un orden explícito del Product Backlog.',
'PB4':'La foto de planificación separa fechas y parámetros; el Excel los reúne en HU3. Se observa aclaración, sin acuerdo fechado.',
'PB5':'El Excel refleja una estructura de HU distinta de la foto previa; falta decisión y trazabilidad del cambio.',
'SP1':'Sprints!B3 y foto 2 muestran objetivo y reunión del equipo; falta acuerdo explícito de aceptación.',
'SP2':'Las HU seleccionadas cubren el objetivo de consulta, reserva, adelanto e ingreso.',
'SB1':'Sprints!D3:E10 y Tablero Scrum Sprint 1 identifican selección y tareas.',
'SB2':'Tareas de HU incluye descomposición en interfaz, API, integración y pruebas.',
'EJ1':'El tablero contiene cortes con Por hacer, Haciendo y Hecho.',
'EJ2':'La bitácora informa incidencias de Mercado Pago y puerto ocupado; faltan registros originales fechados para validar su totalidad.',
'EJ3':'La bitácora y commits de integración registran coordinación de pagos y reservas; falta acuerdo de resolución de la dependencia.',
'EJ4':'No se aporta comparación del Sprint Backlog antes/después de una decisión de adaptación.',
'EJ5':'Existe hoja DoD/DoR, pero no una verificación de DoD ligada al cierre de una HU en la versión histórica.',
'RV1':'Foto 1 demuestra una reserva temporal en ejecución; al no identificar Sprint Review ni versión, no acredita el criterio de Review.',
'RV2':'Las fotos muestran al equipo; no identifican interesados externos participando en una Review.',
'RV3':'No se aporta retroalimentación de interesados sobre el incremento mostrado.',
'RV4':'No se aporta una decisión de trabajo futuro vinculada a esa retroalimentación.',
'IN1':'El commit 776a2ec integra HU5–HU7 y ajustes de pago al 05/11/2026.',
'IN2':'No se verificaron pruebas históricas y cumplimiento de DoD sobre esa versión.',
'IN3':'Foto 1 acredita uso de una parte del flujo: reserva temporal. El pago figura para una siguiente HU; no prueba adelanto ni check-in completos.',
'IN4':'Código e instrucciones disponibles; no se acredita disponibilidad de un incremento utilizable que cumpla DoD. La foto parcial no demuestra esa condición.'}
for i in range(1,7):reasons[f'DS{i}']='Las fotos no identifican un Daily ni acreditan este criterio (frecuencia, duración, inspección o adaptación del plan).'
for i in range(1,5):reasons[f'RT{i}']='No se aporta registro de retrospectiva con reflexión, causa, acción o seguimiento verificable.'
groups=[('Product Backlog',['PB1','PB2','PB3','PB4','PB5']),('Planning y Sprint Backlog',['SP1','SP2','SB1','SB2']),('Ejecución',[f'EJ{i}' for i in range(1,6)]),('Daily Scrum',[f'DS{i}' for i in range(1,7)]),('Sprint Review',[f'RV{i}' for i in range(1,5)]),('Retrospectiva',[f'RT{i}' for i in range(1,5)]),('Incremento',[f'IN{i}' for i in range(1,5)])]
report=['# Evaluación documental preliminar del Sprint 1','Fecha de revisión: 18/11/2026. Evaluador: revisión documental asistida. No constituye una calificación consensuada del equipo.','Fuentes: Historias de usuario.xlsx (versión recibida 16/11), listas de cotejo del Sprint 1, commit 776a2ec y dos capturas de reuniones aportadas por el equipo. Las fotos carecen de fecha visible y no se asignan a eventos oficiales sin evidencia adicional. La bitácora contiene reconstrucciones pendientes de confirmación.','Escala: Cumple 2, Cumplimiento parcial 1, No cumple 0. Sin evidencia verificada: 0. Un 0 no demuestra que la práctica nunca ocurrió. No se excluyen ítems.','Resultado: 20/64 = 31,3 %. 7 ítems cumplen, 6 parciales, 19 sin cumplimiento demostrado. El resultado mide lo acreditado por el conjunto revisado, no el rendimiento del software.','| ID | Criterio original | Valor | Evidencia y límite |','|---|---|---:|---|']
with zipfile.ZipFile('Listas_de_cotejo_Scrum_Sprint_1.docx') as z:
 root=E.fromstring(z.read('word/document.xml'))
 for row in root.xpath('//*[local-name()="tbl"]/*[local-name()="tr"]'):
  cells=[' '.join(c.xpath('.//*[local-name()="t"]/text()')) for c in row.xpath('./*[local-name()="tc"]')]
  if cells and cells[0] in scores:report.append(f'| {cells[0]} | {cells[1]} | {scores[cells[0]]} | {reasons[cells[0]]} |')
report+=['','## Diferencia de estimaciones','La selección actual suma 38 puntos de HU1–HU7 más 4 de HU21: 42. El burndown tiene balance inicial 43 y final 0. Su serie planificada suma 44 y termina en −1. La captura de planificación tiene otra estructura de HU. Se conservan ambas fuentes sin imputar que sus unidades son idénticas. No se calcula velocidad ni puntos aceptados.','## Respaldo de demostración','La foto 1 es una captura real de una demostración parcial. El botón de adelanto anuncia disponibilidad en una siguiente HU. No se presenta como evidencia de pago aprobado, check-in completo ni versión 776a2ec en ejecución. Se incluyen secuencias del Excel como material explicativo.']
(B/'final/Evidencias_y_resultados_Sprint_1.md').write_text('\n\n'.join(report),encoding='utf-8')
(B/'build/resultados.json').write_text(json.dumps({'scores':scores,'reasons':reasons,'groups':groups},ensure_ascii=False),encoding='utf-8')
# Text corrections and new speaking allocation.
edit(1,{12:'Evaluación del desarrollo del sistema de reservas de Sol de Shirá mediante listas de chequeo Scrum',1000:'ARTÍCULO CORTO · EVALUACIÓN DOCUMENTAL PRELIMINAR'},3200)
edit(2,{14:'Carlos Gonzales\nScrum Master\nPresenta: Scrum y listas de chequeo',29:'Julio Pantoja\nProduct Owner\nPresenta: antecedentes y planificación',30:'Miguel Acevedo\nDeveloper · análisis y pruebas\nPresenta: arquitectura y métricas',3008:'Antonella Baldeon\nDeveloper · base de datos\nPresenta: introducción y problema',3004:'Lee Orbezo\nDeveloper · desarrollo\nPresenta: demostración y cierre',1003:'Equipo: Sol de Shirán · Docente: Omar Loayza'},1800)
edit(3,{25:'Antonella: introducción y problema · 3 min',29:'Julio: antecedentes y planificación · 3 min',33:'Carlos: Scrum y evaluación documental · 3 min',37:'Miguel: arquitectura y métricas · 3 min'},2600)
r=read('ppt/slides/slide3.xml');native(r,2400,'Lee: demostración y conclusiones · 3 min',8200000,8700000,8500000,600000,2400,False);save('ppt/slides/slide3.xml',r)
edit(4,{32:'Objetivo: evaluar documentalmente el proceso Scrum del Sprint 1. El producto busca centralizar el hospedaje, de la consulta al ingreso.',34:'Organización\nComplejo Turístico Sol de Shirá, Trujillo.\nOfrece hospedaje y recreación.\nRecibe consultas por WhatsApp y registros manuales.\nExige un adelanto del 50 %.',35:'Problema y aporte\nInformación dispersa y riesgo de cruce de reservas.\nSin mediciones previas de tiempos o errores.\nAporte: trazabilidad de 10 RF y 4 RNF, arquitectura y evaluación documental.'},2200)
edit(6,{1002:'Objetivo: digitalizar las reservas.\nSprint 1: consulta, reserva, adelanto y check-in.\nEquipo: PO, SM y 3 Developers.\nRiesgos: pasarela e integración de pagos.\nRestaurante: alcance posterior.',1004:'Matriz: 10 RF y 4 RNF.\nTrazabilidad: requisito, HU y prueba.\nSelección actual: 8 HU, 42 puntos nominales.\nLas versiones difieren en códigos de HU.\nCorte: 776a2ec, 05/11/2026.'},2200)
edit(7,{1001:'Evaluación documental\n32 criterios en siete ámbitos.\nCumple: 2. Parcial: 1. No cumple: 0.\nSin evidencia verificada: 0.',1005:'Roles y planificación\nPO: Julio. SM: Carlos.\nObjetivo y selección documentados.\nDuración del Sprint: por confirmar.',1009:'Eventos\nLas fotos muestran planificación y demostración.\nNo acreditan frecuencia ni duración de los Daily, ni acuerdos de retrospectiva.',1013:'Artefactos\nProduct Backlog, Sprint Backlog e Incremento.\nLa DoD define calidad.\nEl porcentaje es preliminar y requiere validación del equipo.'},2200)
edit(10,{1001:'42 pts\nSelección nominal del Excel',1002:'8 HU\nSelección actual, aceptación por verificar',1003:'43 ud.\nInicio del burndown, unidad sin conciliar',1004:'0 ud.\nFinal registrado, no acredita DoD',1007:'Son medidas diferentes hasta demostrar equivalencia. La serie planificada termina en −1. Se conserva el dato fuente y no se calcula velocidad.',1008:'Burndown del Sprint 1'},1800)
r=read('ppt/slides/slide10.xml')
for sid in [1001,1002,1003,1004]:
 for j,p in enumerate(shape(r,sid).findall('p:txBody/a:p',N)):
  for rp in p.findall('.//a:rPr',N):rp.set('sz','3600' if j==0 else '1800')
save('ppt/slides/slide10.xml',r)
edit(11,{1001:'Acceso\nCorreo, contraseña y rol.',1005:'Reserva\nCabaña, fechas y solicitud.',1009:'Adelanto\nMostrar solo en entorno de prueba.',1013:'Ingreso\nConsultar saldo y check-in.',1017:'Proceso\nMostrar tablero y evidencia.',1021:'Respaldo\nCaptura real en la siguiente lámina.',1030:'Usar el corte del Sprint 1 si está preparado. La captura de respaldo acredita solo una reserva temporal.'},2200)
edit(12,{1004:'Resultado: 20/64 = 31,3 % de cumplimiento documental preliminar en 32 criterios. No es una valoración consensuada.',1008:'Fortalezas: historias, objetivo, tareas e integración identificable. Las fotos respaldan planificación y una demostración parcial.',1012:'Pendientes: evidencias de Daily, Review y retrospectiva; DoD del corte histórico y diferencia entre 42 puntos y 43 unidades.',1016:'Siguiente paso: validar la calificación con el equipo, revisar acuerdos, reproducir pruebas y conciliar estimaciones.'},2200)
# Two result slides, based on the user's wide table layout.
clone(5,15);edit(15,{33:'RESULTADOS DEL CHECKLIST',1000:'C8   Evaluación documental preliminar · 32 criterios',1002:'20/64 = 31,3 %. Siete cumplen, seis parciales y diecinueve sin cumplimiento demostrado. Sin evidencia verificada se asigna 0; no significa que el evento no ocurrió.'},2200)
r=read('ppt/slides/slide15.xml');tb=r.find('.//a:tbl',N);rows=tb.findall('a:tr',N);head,proto=copy.deepcopy(rows[0]),copy.deepcopy(rows[1])
for rr in rows:tb.remove(rr)
vals=[['Ámbito','Ítems','Puntos','Máximo','Resultado']]
for name,ids in groups:vals.append([name,str(len(ids)),str(sum(scores[x] for x in ids)),str(2*len(ids)),f'{100*sum(scores[x] for x in ids)/(2*len(ids)):.1f} %'.replace('.',',')])
vals.append(['Global','32','20','64','31,3 %'])
for j,vs in enumerate(vals):
 rr=copy.deepcopy(head if j==0 else proto);rr.set('h','420000')
 for c,v in zip(rr.findall('a:tc',N),vs):fill(c.find('a:txBody',N),v,1800)
 tb.append(rr)
gf=r.find('.//p:graphicFrame',N);geom(gf,1900000,3250000,14500000,3780000);save('ppt/slides/slide15.xml',r)
clone(8,16);edit(16,{40:'EVIDENCIA Y VALORACIÓN',1000:'C8   Ejemplos de aplicación de la escala',1002:'SB1 · Cumple (2)\nExcel y tablero identifican\nlas HU seleccionadas.\n\nIN3 · Parcial (1)\nLa foto acredita una reserva temporal; no el flujo completo.\n\nDS2 · No cumple (0)\nNo hay intervalo de inicio y fin de un Daily.'},2200)
r=read('ppt/slides/slide16.xml')
for p in r.findall('.//p:pic',N):p.getparent().remove(p)
# Place the real planning screenshot in the old architecture image area.
geom(shape(r,1002),10800000,3900000,5700000,5000000);save('ppt/slides/slide16.xml',r)
image(16,'C:/Users/tania/AppData/Local/Temp/codex-clipboard-393654d7-1804-4441-b364-628f7311570d.png',1900000,4000000,8400000,4800000,'Captura real de planificación aportada por el equipo')
# Replace the small C4 image with a large, editable summary.
edit(8,{40:'ARQUITECTURA C4',1000:'C6   Contenedores del Sprint 1 · resumen del corte 776a2ec'})
r=read('ppt/slides/slide8.xml');shape(r,1002).getparent().remove(shape(r,1002))
for p in r.findall('.//p:pic',N):p.getparent().remove(p)
native(r,2100,'Aplicación web Angular',6500000,3900000,5200000,650000)
native(r,2101,'API Gateway',6500000,4700000,5200000,650000)
line(r,2300,9100000,4550000,9100000,4700000)
for j,(name,detail) in enumerate([('auth-service','Autenticación'),('usuario-service','Cuentas y roles'),('catalogo-service','Cabañas'),('reservas-service','Estancias'),('pagos-service','Adelanto y saldo')]):
 x=1400000+j*3100000;native(r,2110+j,name+'\n'+detail,x,6000000,2900000,1100000,2000);line(r,2310+j,9100000,5350000,x+1450000,6000000)
native(r,2150,'MySQL: usuario_db, db_catalogo, db_reservas y db_pagos',3000000,7700000,12600000,700000,2100)
for j in range(1,5):line(r,2350+j,1400000+j*3100000+1450000,7100000,1400000+j*3100000+1450000,7700000)
native(r,2160,'Infraestructura: Eureka y Config Server. Sistema externo: Mercado Pago. JWT y BCrypt para seguridad.',1900000,8750000,14500000,500000,2000,False);save('ppt/slides/slide8.xml',r)
# Large native logical data diagram instead of an unreadable screenshot.
clone(8,17);edit(17,{40:'MODELO DE DATOS',1000:'C6   Relaciones lógicas verificadas en el código del Sprint 1'})
r=read('ppt/slides/slide17.xml');tree=r.find('p:cSld/p:spTree',N)
for p in list(tree):
 node=p.find('p:nvSpPr/p:cNvPr',N) if E.QName(p).localname=='sp' else None
 if E.QName(p).localname=='cxnSp' or (node is not None and int(node.get('id'))>=2100):tree.remove(p)
for sid,txt,x in [(2200,'cabana\nIdentidad y tarifa\nAforo y estado',1700000),(2201,'reserva\nCabaña y usuario por ID\nFechas, monto y estado',6800000),(2202,'transaccion_pago\nReserva por ID\nMonto y estado de pago',11900000)]:native(r,sid,txt,x,4000000,4500000,2000000,2400)
line(r,2400,6200000,5000000,6800000,5000000);line(r,2401,11300000,5000000,11900000,5000000)
native(r,2210,'pago_restante\nSaldo presencial y clave de idempotencia\nEntidad adicional del código histórico',2500000,6800000,6500000,1500000,2200)
native(r,2211,'webhook_log\nEventos de la pasarela\nNo asumir una FK propuesta como implementada',10000000,6800000,6500000,1500000,2200)
native(r,2212,'Referencias lógicas entre dominios. La imagen recibida usa tipos Oracle; la configuración histórica utiliza MySQL.',1900000,8750000,14500000,600000,2000,False);save('ppt/slides/slide17.xml',r)
# Actual screenshot for offline presentation, without claiming a completed payment.
clone(5,18);edit(18,{33:'RESPALDO DE DEMOSTRACIÓN',1000:'E3   Captura real aportada por el equipo',1002:'La pantalla acredita una reserva temporal. El botón anuncia el adelanto para una siguiente HU. No prueba pago aprobado, check-in ni ejecución del commit 776a2ec.'},2200)
r=read('ppt/slides/slide18.xml')
for p in r.findall('.//p:graphicFrame',N):p.getparent().remove(p)
save('ppt/slides/slide18.xml',r)
image(18,'C:/Users/tania/AppData/Local/Temp/codex-clipboard-2dc6b191-6fb2-4838-b824-a26d869611b8.png',1900000,3300000,14500000,4100000,'Captura real de demostración de reserva temporal')
for sn,sid in [(15,33),(16,40),(18,33)]:
 rr=read(f'ppt/slides/slide{sn}.xml')
 for rp in shape(rr,sid).findall('.//a:rPr',N):rp.set('sz','4800')
 save(f'ppt/slides/slide{sn}.xml',rr)
# Native slide order matches five contiguous speaking parts.
order=[1,2,3,4,5,6,9,7,15,16,8,17,10,11,18,12,13,14];seconds=[20,25,15,120,55,75,50,60,80,40,75,45,60,105,30,30,10,5];assert sum(seconds)==900
pres=read('ppt/presentation.xml');ids=pres.find('p:sldIdLst',N);old=list(ids);rels=read('ppt/_rels/presentation.xml.rels');mapping={i+1:el for i,el in enumerate(old)}
for num in [15,16,17,18]:
 rid='rIdNew'+str(num);E.SubElement(rels,'{'+RN+'}Relationship',Id=rid,Type=N['r']+'/slide',Target=f'slides/slide{num}.xml');el=E.Element(q('p:sldId'),id=str(6000+num));el.set(q('r:id'),rid);mapping[num]=el
for el in old:ids.remove(el)
for num in order:ids.append(mapping[num])
save('ppt/presentation.xml',pres);save('ppt/_rels/presentation.xml.rels',rels)
for i,(num,sec) in enumerate(zip(order,seconds),1):
 k=f'ppt/notesSlides/notesSlide{num}.xml';r=read(k)
 for sp in r.findall('.//p:sp',N):
  ph=sp.find('p:nvSpPr/p:nvPr/p:ph',N)
  if ph is not None and ph.get('type')=='body':fill(sp.find('p:txBody',N),f'Diapositiva {i}/18. Tiempo: {sec} segundos. Evaluación documental preliminar: 20/64 = 31,3 %. Consultar Evidencias_y_resultados_Sprint_1.md y el guion. Fuentes: Excel recibido, listas de cotejo, código 776a2ec y dos capturas del equipo. No atribuir a las fotos una fecha, duración o evento no acreditados. No interpretar 42 puntos y 43 unidades como medidas equivalentes.')
 save(k,r)
if 'docProps/app.xml' in F:
 r=read('docProps/app.xml')
 for el in r.iter():
  if E.QName(el).localname=='Slides':el.text='18'
 save('docProps/app.xml',r)
with zipfile.ZipFile(B/'build/candidate.pptx','w',zipfile.ZIP_DEFLATED) as z:
 for k,v in F.items():z.writestr(k,v)
print('18 slides; 900 seconds; 20/64, 31.25%')

