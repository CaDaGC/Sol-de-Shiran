import zipfile,copy
from pathlib import Path
from lxml import etree as E
from PIL import Image
b=Path('outputs/presentacion_sprint1');n={'a':'http://schemas.openxmlformats.org/drawingml/2006/main','p':'http://schemas.openxmlformats.org/presentationml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'};relNS='http://schemas.openxmlformats.org/package/2006/relationships';ctNS='http://schemas.openxmlformats.org/package/2006/content-types'
def q(k):a,z=k.split(':');return '{'+n[a]+'}'+z
def xml(k):return E.fromstring(f[k])
def save(k,r):f[k]=E.tostring(r,xml_declaration=True,encoding='UTF-8',standalone=True)
def put(sp,text,size=None):
 tx=sp.find('p:txBody',n);ps=tx.findall('a:p',n);pr=ps[0].find('a:pPr',n) if ps else None;rp=tx.find('.//a:rPr',n);rp=copy.deepcopy(rp) if rp is not None else E.Element(q('a:rPr'))
 if size:rp.set('sz',str(size))
 for p in ps:tx.remove(p)
 for line in text.split('\n'):
  p=E.SubElement(tx,q('a:p'))
  if pr is not None:p.append(copy.deepcopy(pr))
  run=E.SubElement(p,q('a:r'));run.append(copy.deepcopy(rp));E.SubElement(run,q('a:t')).text=line

def changes(num,mapping):
 k=f'ppt/slides/slide{num}.xml';r=xml(k)
 for sp in r.findall('.//p:sp',n):
  sid=int(sp.find('p:nvSpPr/p:cNvPr',n).get('id'))
  if sid in mapping:put(sp,mapping[sid])
 save(k,r)
with zipfile.ZipFile(b/'final/Presentacion_Sprint_1_Sol_de_Shira.pptx') as z:f={k:z.read(k) for k in z.namelist()}
changes(3,{25:'Problema y antecedentes · 3 min',29:'Método y evaluación de Scrum · 6 min',33:'Resultados y demostración · 5 min',37:'Discusión y conclusiones · 1 min',1000:'E1   15 min de exposición. Preguntas al finalizar.'})
changes(14,{1000:'¿Preguntas?'})
# Reuse the original wide-content slide layout for two evidence figures.
for number,title,subtitle,img,caption in [
 (15,'PROCESO SCRUM','C5   Inspección y adaptación durante el Sprint 1','outputs/articulo_sprint1/scrum.png','La lista verifica el propósito de cada práctica. El Sprint contiene los eventos. El equipo debe confirmar los acuerdos y la Definition of Done.'),
 (16,'MODELO DE DATOS','C6   Evidencia de modelado del primer incremento','outputs/articulo_sprint1/diagramas/base_de_datos_recibida.png','Modelo recibido del equipo. Las relaciones entre dominios son lógicas. Los tipos y campos deben conciliarse con las entidades MySQL del corte histórico.')]:
 k=f'ppt/slides/slide{number}.xml';r=xml('ppt/slides/slide5.xml');tree=r.find('p:cSld/p:spTree',n)
 for gf in r.findall('.//p:graphicFrame',n):gf.getparent().remove(gf)
 for sp in r.findall('.//p:sp',n):
  sid=sp.find('p:nvSpPr/p:cNvPr',n).get('id')
  if sid=='33':put(sp,title)
  if sid=='1000':put(sp,subtitle)
  if sid=='1002':put(sp,caption)
 # Keep title, logos, footer and existing caption band from the template.
 rel=xml('ppt/slides/_rels/slide5.xml.rels')
 for entry in list(rel):
  if entry.get('Type','').endswith('/notesSlide'):entry.set('Target',f'../notesSlides/notesSlide{number}.xml')
 rid=f'rIdImage{number}';E.SubElement(rel,'{'+relNS+'}Relationship',Id=rid,Type=n['r']+'/image',Target=f'../media/figure-{number}.png')
 iw,ih=Image.open(img).size;x,y,w,h=2000000,3400000,14000000,3900000;scale=min(w/iw,h/ih);nw,nh=int(iw*scale),int(ih*scale)
 pic=E.fromstring(f'''<p:pic xmlns:p="{n['p']}" xmlns:a="{n['a']}" xmlns:r="{n['r']}"><p:nvPicPr><p:cNvPr id="2001" name="{title}"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="{x+(w-nw)//2}" y="{y+(h-nh)//2}"/><a:ext cx="{nw}" cy="{nh}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic>''');tree.append(pic);save(k,r);save(f'ppt/slides/_rels/slide{number}.xml.rels',rel);f[f'ppt/media/figure-{number}.png']=Path(img).read_bytes()
 f[f'ppt/notesSlides/notesSlide{number}.xml']=f['ppt/notesSlides/notesSlide5.xml']
 nr=xml('ppt/notesSlides/_rels/notesSlide5.xml.rels')
 for entry in nr:
  if entry.get('Type','').endswith('/slide'):entry.set('Target',f'../slides/slide{number}.xml')
 save(f'ppt/notesSlides/_rels/notesSlide{number}.xml.rels',nr)
 ct=xml('[Content_Types].xml')
 for name,kind in [(f'/ppt/slides/slide{number}.xml','slide'),(f'/ppt/notesSlides/notesSlide{number}.xml','notesSlide')]:E.SubElement(ct,'{'+ctNS+'}Override',PartName=name,ContentType=f'application/vnd.openxmlformats-officedocument.presentationml.{kind}+xml')
 save('[Content_Types].xml',ct)
# Insert the new slides into the existing presentation order.
pres=xml('ppt/presentation.xml');ids=pres.find('p:sldIdLst',n);rels=xml('ppt/_rels/presentation.xml.rels');original=list(ids)
for number,after in [(15,7),(16,8)]:
 rid=f'rIdAdded{number}';E.SubElement(rels,'{'+relNS+'}Relationship',Id=rid,Type=n['r']+'/slide',Target=f'slides/slide{number}.xml');sid=E.Element(q('p:sldId'),id=str(5000+number));sid.set(q('r:id'),rid);ids.insert(list(ids).index(original[after-1])+1,sid)
save('ppt/presentation.xml',pres);save('ppt/_rels/presentation.xml.rels',rels)
order=[1,2,3,4,5,6,7,15,8,16,9,10,11,12,13,14]
seconds=[20,25,15,70,55,65,90,60,75,50,45,85,160,60,15,10]
assert sum(seconds)==900
scripts={
1:'Presentamos la evaluación del proceso de desarrollo del sistema de gestión de reservas de Sol de Shirá. El análisis corresponde exclusivamente al Sprint 1 y utiliza listas de chequeo para revisar evidencias observables.',
2:'Julio Pantoja cumple el rol de Product Owner y Carlos Gonzales el de Scrum Master. Miguel Acevedo, Lee Orbezo y Antonella Baldeon integran el trabajo de Developers con especialidades de pruebas, desarrollo y base de datos. Estas especialidades no crean roles Scrum adicionales.',
3:'La exposición dura quince minutos. Primero describimos el problema y los antecedentes. Luego explicamos el método y las evidencias del primer sprint. Finalmente presentamos los resultados documentados, una demostración y las conclusiones.',
4:'El centro campestre se encuentra en Trujillo y ofrece hospedaje y recreación. El proceso descrito comienza con consultas por WhatsApp. El personal revisa disponibilidad y solicita un adelanto del cincuenta por ciento. La información dispersa aumenta el riesgo de cruces de fechas y de coordinación manual. El objetivo de la evaluación es revisar cómo aplicamos Scrum al desarrollar el primer incremento. No contamos con una línea base que permita afirmar una reducción porcentual de errores o tiempos. Por ello, nuestra contribución se concentra en trazabilidad y evidencia del proceso.',
5:'La Guía Scrum 2020 es la referencia principal. Kniberg aporta ejemplos de aplicación práctica que deben adaptarse al contexto. Alami y Krancher relacionan la calidad con colaboración y verificación. Hron y Obwegeser estudian cómo se adapta Scrum. Estos antecedentes justifican contrastar el tablero con el incremento y las pruebas, y documentar las adaptaciones académicas. No constituyen una medición directa de los resultados del campestre.',
6:'El Project Charter plantea digitalizar la gestión de reservas. Para esta evaluación delimitamos el alcance al catálogo, la reserva, el adelanto y el ingreso presencial del Sprint 1. La matriz normalizada reúne diez requisitos funcionales y cuatro no funcionales. Relacionamos los requisitos con las historias y el código del commit 776a2ec, del cinco de noviembre. Los riesgos principales incluyen la dependencia de la pasarela y la coordinación entre estados de pago y ocupación. Las modificaciones posteriores, como el registro público de clientes, quedan fuera de este análisis.',
7:'Las listas de chequeo organizan la evaluación en siete ámbitos. Integramos planificación y elaboración del Sprint Backlog en un mismo ámbito para mantener siete instrumentos. Los demás son Product Backlog, ejecución, Daily Scrum, Review, retrospectiva e incremento. La escala es Cumple con dos puntos, Cumplimiento parcial con uno y No cumple con cero. El parcial requiere identificar qué parte se cumple y qué queda pendiente. Sin evidencia verificable corresponde cero. El porcentaje se calcula como puntaje obtenido dividido entre dos por el número de ítems aplicables, multiplicado por cien. Las exclusiones requieren justificación. Aún no presentamos una puntuación global validada. Ejemplo de criterio: el Sprint Backlog contiene un objetivo explícito. La evidencia debe ser el documento o registro donde ese objetivo aparece, no solo la asistencia a una reunión.',
15:'El esquema muestra la relación entre necesidades, planificación, trabajo del sprint e incremento. El Sprint es el evento contenedor y el Daily ocurre durante su ejecución. La Review permite inspeccionar resultados con los interesados y adaptar el Product Backlog. La retrospectiva se centra en mejorar la forma de trabajo. El refinamiento es una actividad continua. La ilustración no establece una secuencia obligatoria de liberación: un incremento que cumple la Definition of Done puede entregarse antes del final del sprint. Una captura del tablero demuestra estados de trabajo, pero no acredita por sí sola el propósito de los eventos.',
8:'El diagrama C4 resume los contenedores del primer incremento. Angular consume el API Gateway. Los servicios de autenticación, usuarios, catálogo, reservas y pagos atienden los dominios. MySQL conserva la información. Eureka y Config Server apoyan el descubrimiento y la configuración. Mercado Pago interviene en el adelanto. El uso de JWT y BCrypt corresponde al control de acceso y al almacenamiento de contraseñas. Localizamos cinco escenarios de pruebas asociados a consulta, detalle, check-in y pagos. Su presencia en el repositorio no equivale a afirmar que todas las pruebas históricas fueron ejecutadas correctamente.',
16:'La imagen procede del modelo de datos enviado por el equipo. Incluye cabaña, reserva, transacción de pago y registro de webhooks. Usuario aparece como referencia externa. Las relaciones entre dominios se interpretan como referencias lógicas. La captura contiene tipos y nombres que difieren del código MySQL histórico, y la relación opcional del webhook al pago aparece como propuesta. Antes de tratar esta imagen como DDL definitivo necesitamos conciliarla con las entidades reales. El código del primer sprint también contiene una entidad para el saldo presencial.',
9:'Scrum organiza el trabajo mediante sprints, responsabilidades, eventos y artefactos. OpenUP sirve como referencia comparativa para organizar requisitos, arquitectura, desarrollo y pruebas. No afirmamos haber aplicado íntegramente ambos enfoques. Las especialidades de análisis, pruebas y base de datos se mantienen dentro del trabajo de Developers. La documentación complementaria debe conectarse con el incremento y sus criterios de calidad.',
10:'La selección del primer sprint comprende ocho historias y suma cuarenta y dos puntos nominales. El burndown comienza en cuarenta y tres unidades y termina en cero. Esa diferencia impide presentar la gráfica como una velocidad verificada. Tampoco contamos con un porcentaje histórico de pruebas aprobadas. El equipo dispone de un video de entrevista, pero aún falta vincularlo con la aceptación de una versión identificable. El resultado que sí podemos sostener es la localización del alcance en documentos y código. Para cerrar la evaluación necesitamos reconciliar las métricas y aplicar las listas con evidencias suficientes. Los estados del tablero y el código integrado son evidencias complementarias, no sustitutos de la aceptación de cada historia.',
11:'Demostración, 160 segundos. En los primeros 25 segundos mostrar el acceso por rol. Durante los siguientes 30 segundos consultar catálogo, una cabaña y disponibilidad. En 35 segundos mostrar una solicitud de reserva y el inicio del adelanto del cincuenta por ciento, usando entorno de prueba. En 30 segundos mostrar la consulta de la reserva, el saldo y el check-in. Dedicar los últimos 40 segundos al tablero y a un criterio de la lista de chequeo: mostrar la evidencia, explicar el estado que justificaría y distinguirlo de una simple tarea marcada como hecha. La versión objetivo es el corte del Sprint 1. Si no está preparada para ejecutar, explicar el flujo con capturas disponibles sin afirmar una ejecución en vivo. Evitar utilizar funciones posteriores como evidencia de este sprint.',
12:'La principal conclusión es que podemos relacionar requisitos, historias y componentes del Sprint 1 mediante una matriz de trazabilidad. Las listas permiten evaluar prácticas con una escala común, pero requieren consenso y aplicación sustentada. Las diferencias entre estimaciones y burndown, los acuerdos de eventos por verificar y la aceptación pendiente limitan las conclusiones. El siguiente paso es reproducir el commit histórico, ejecutar las pruebas críticas y conciliar las métricas. Después podremos calcular el porcentaje por ámbito y el global. No atribuimos una mejora operacional sin mediciones comparables.',
13:'Las referencias principales son la Guía Scrum 2020 y el libro de Kniberg. Los estudios complementarios fundamentan la importancia de la verificación y de explicitar las adaptaciones.',
14:'Gracias. Abrimos el espacio de preguntas al finalizar los quince minutos de exposición.'}
for position,(number,sec) in enumerate(zip(order,seconds),1):
 k=f'ppt/notesSlides/notesSlide{number}.xml';r=xml(k)
 body=next((sp for sp in r.findall('.//p:sp',n) if sp.find('p:nvSpPr/p:nvPr/p:ph',n) is not None and sp.find('p:nvSpPr/p:nvPr/p:ph',n).get('type')=='body'),None)
 if body is not None:put(body,f'Duración sugerida: {sec} segundos. Diapositiva {position} de 16.\n\n{scripts[number]}\n\nFuente: artículo del Sprint 1, Historias de usuario.xlsx y código 776a2ec del 05/11/2026. Diagramas del artículo del equipo. Guía Scrum 2020 y Kniberg (2015).');save(k,r)
# Update application metadata slide count.
if 'docProps/app.xml' in f:
 r=xml('docProps/app.xml')
 for el in r.iter():
  if E.QName(el).localname=='Slides':el.text='16'
 save('docProps/app.xml',r)
out=b/'build/candidate_15min.pptx'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for k,v in f.items():z.writestr(k,v)
print(f'{out}: {len(order)} diapositivas, {sum(seconds)} segundos')
