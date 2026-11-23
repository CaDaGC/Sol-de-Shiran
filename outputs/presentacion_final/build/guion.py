from pathlib import Path
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,PageBreak
from reportlab.lib.styles import getSampleStyleSheet,ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from xml.sax.saxutils import escape
import json,zipfile
from lxml import etree as E
B=Path('outputs/presentacion_final')
parts=[
('Antonella Baldeon','Diapositivas 1–4 · 3 minutos · Introducción y problema',[
('1. Presentación (20 s)','Buenos días, profesor y compañeros. Presentamos la evaluación del proceso de desarrollo del sistema de reservas del Complejo Turístico Sol de Shirá. El trabajo se concentra en el primer sprint y utiliza listas de chequeo para revisar las evidencias disponibles.'),
('2. Equipo (25 s)','Nuestro equipo está formado por Carlos Gonzales como Scrum Master, Julio Pantoja como Product Owner, y Miguel Acevedo, Lee Orbezo y quien les habla como Developers. Cada uno aporta una especialidad: análisis y pruebas, desarrollo o base de datos.'),
('3. Agenda (15 s)','Primero explicaré el problema del negocio. Julio presentará los antecedentes y la planificación. Carlos explicará la evaluación de Scrum. Miguel mostrará la arquitectura y las métricas. Lee cerrará con la demostración y las conclusiones.'),
('4. Problema del negocio (2 min)','Sol de Shirá se encuentra en Trujillo y ofrece hospedaje y espacios recreativos. Según la información proporcionada por el equipo, las consultas y reservas se coordinan mediante WhatsApp y registros manuales. Para confirmar una reserva, el negocio exige un adelanto del cincuenta por ciento.\n\nEl problema aparece cuando la información está dispersa y el personal debe revisar varios registros para conocer la disponibilidad. Esto genera riesgo de demoras y cruces de reservas. El sistema busca centralizar este proceso, desde la consulta de una cabaña hasta el ingreso del huésped.\n\nAdemás del producto, en este trabajo revisamos cómo organizamos su desarrollo con Scrum. No contamos con una medición previa para afirmar cuánto disminuyeron los tiempos o los errores. Por eso, los resultados que presentamos se basan en documentos, código y capturas del trabajo. Julio continuará con los antecedentes y la planificación.')]),
('Julio Pantoja','Diapositivas 5–7 · 3 minutos · Antecedentes, inicio y OpenUP',[
('5. Trabajos relacionados (55 s)','Los antecedentes relacionan la calidad en Scrum con colaboración, transparencia e inspección. Los estudios de Alami y Krancher, Hron y Obwegeser y los otros autores de la tabla nos orientan a revisar las adaptaciones del equipo y sus evidencias.\n\nPara nuestro caso, una tarea marcada como hecha debe contrastarse con una versión integrada y sus pruebas. Estos trabajos aportan fundamentos, pero no demuestran una mejora medida en el campestre.'),
('6. Inicio del proyecto (75 s)','El Project Charter plantea digitalizar las reservas. En el Sprint 1 delimitamos el alcance a consulta, reserva, adelanto y check-in. Los pedidos de restaurante corresponden a un alcance posterior.\n\nLa matriz de este corte contiene diez requisitos funcionales y cuatro no funcionales. La trazabilidad relaciona requisitos, historias y pruebas. La selección actual del Excel tiene ocho historias que suman cuarenta y dos puntos nominales.\n\nLos documentos tienen diferencias en algunos códigos de historias. Por eso usamos también el nombre y la función para identificarlas. Los principales riesgos incluyen la pasarela y la coordinación entre estados de pago y ocupación. El análisis técnico se fija en el commit 776a2ec, del cinco de noviembre.'),
('7. Scrum y OpenUP (50 s)','Scrum organiza el trabajo mediante sprints, eventos, responsabilidades y artefactos. OpenUP se utiliza como referencia para ordenar requisitos, arquitectura, desarrollo y pruebas. No afirmamos que el equipo haya aplicado íntegramente ambos enfoques.\n\nLas especialidades de análisis, pruebas o base de datos permanecen dentro del trabajo de Developers. El propósito de la comparación es vincular la documentación con el incremento y con sus riesgos. Carlos explicará cómo evaluamos las prácticas Scrum.')]),
('Carlos Gonzales','Diapositivas 8–10 · 3 minutos · Método y resultados del checklist',[
('8. Gestión con Scrum (60 s)','La evaluación utiliza las siete listas de cotejo existentes y sus treinta y dos criterios. Integramos Sprint Planning y Sprint Backlog en un mismo ámbito. La escala propia del instrumento asigna dos puntos a Cumple, uno a Cumplimiento parcial y cero a No cumple.\n\nCada calificación necesita evidencia. Si no existe evidencia suficiente en el conjunto revisado, corresponde cero. Esto no demuestra que una reunión nunca ocurrió. Indica que su cumplimiento no quedó acreditado. El Product Backlog, el Sprint Backlog y el Incremento son los artefactos de Scrum; la Definition of Done establece la calidad exigida al incremento.'),
('9. Resultados (80 s)','La revisión documental preliminar obtiene veinte puntos de un máximo de sesenta y cuatro, equivalentes a treinta y uno coma tres por ciento al mostrar un decimal. Siete criterios cumplen, seis presentan cumplimiento parcial y diecinueve no tienen cumplimiento demostrado.\n\nLa planificación y el Sprint Backlog concentran el mayor respaldo, con ochenta y siete coma cinco por ciento. Hay historias seleccionadas, objetivo y tareas. El Product Backlog obtiene sesenta por ciento y la ejecución cuarenta por ciento.\n\nDaily, Review y retrospectiva tienen cero en esta revisión porque no se aportan registros suficientes de sus propósitos. Las fotografías muestran reuniones y una demostración, pero no identifican esos eventos ni sus acuerdos.\n\nEsta es una valoración documental preliminar. El equipo debe revisarla criterio por criterio y aportar evidencias adicionales antes de presentarla como calificación consensuada.'),
('10. Ejemplos de evidencia (40 s)','Aquí mostramos la captura real de planificación enviada por el equipo. Junto con el Excel y el tablero, respalda la identificación de las historias seleccionadas: dos puntos.\n\nLa captura de reserva temporal respalda una parte del flujo: un punto para el uso parcial del producto. En cambio, una hora visible no permite calcular cuánto duró un Daily, por lo que ese criterio recibe cero. Miguel presentará la arquitectura y las métricas del producto.')]),
('Miguel Acevedo','Diapositivas 11–13 · 3 minutos · Arquitectura, datos y métricas',[
('11. Arquitectura C4 (75 s)','El diagrama muestra los contenedores del primer incremento. Angular es la aplicación web. El API Gateway recibe las solicitudes y las dirige a cinco servicios funcionales: autenticación, usuarios, catálogo, reservas y pagos.\n\nMySQL conserva los datos por dominio. Eureka apoya el descubrimiento de servicios y Config Server centraliza la configuración. Mercado Pago interviene en el adelanto. JWT y BCrypt apoyan la autenticación y el almacenamiento de contraseñas.\n\nEl diagrama resume la versión histórica 776a2ec. No incorpora funcionalidades posteriores como evidencia del Sprint 1.'),
('12. Modelo de datos (45 s)','Este resumen lógico muestra cabaña, reserva y transacción de pago. La reserva referencia a la cabaña y al usuario mediante identificadores. El código también contiene pago_restante para el saldo presencial y el control de idempotencia.\n\nWebhook_log registra eventos de la pasarela. Las referencias entre dominios no prueban por sí mismas claves foráneas físicas. La imagen de modelado recibida usa tipos Oracle, mientras la configuración histórica utiliza MySQL; esa diferencia necesita conciliación.'),
('13. Métricas (60 s)','La selección actual reúne ocho historias y cuarenta y dos puntos nominales. El burndown comienza en cuarenta y tres unidades y termina en cero. No tenemos una definición que permita considerar ambas medidas equivalentes. Además, la serie planificada termina en menos uno.\n\nPor eso conservamos los datos originales y no calculamos velocidad ni afirmamos que el cero demuestra aceptación de todas las historias. Los estados del tablero, los puntos y el cumplimiento de la Definition of Done son evidencias diferentes. La conciliación de las métricas y la ejecución histórica de pruebas siguen pendientes. Lee mostrará la demostración y cerrará la exposición.')]),
('Lee Orbezo','Diapositivas 14–18 · 3 minutos · Demostración y cierre',[
('14. Demostración (105 s)','Si el entorno del Sprint 1 está preparado, mostrar acceso, cabaña y fechas, solicitud, adelanto en entorno de prueba y consulta de la reserva. No realizar un cobro real ni mostrar funcionalidades de sprints posteriores como evidencia del primero.\n\nFrases de apoyo: «Primero ingresamos según el rol. Consultamos una cabaña y las fechas. Registramos la solicitud. El adelanto se procesa en entorno de prueba. Desde el acceso autorizado consultamos la reserva y las opciones de saldo e ingreso».\n\nSi el entorno no está preparado, pasar a la siguiente lámina y explicar el respaldo documental. No afirmar que una ejecución en vivo ocurrió.'),
('15. Captura de respaldo (30 s)','Esta es una captura real aportada por el equipo. Se observa la reserva temporal y la participación en una sesión de demostración. El botón anuncia el adelanto para una siguiente historia. Por eso, la captura respalda una parte funcional, pero no acredita pago aprobado, check-in completo ni la versión integrada del cinco de noviembre.'),
('16. Conclusiones (30 s)','La principal conclusión es que existe trazabilidad documental y una integración identificable, pero faltan evidencias para acreditar varias prácticas Scrum. La revisión preliminar obtiene treinta y uno coma tres por ciento en el instrumento. El siguiente paso es validar las calificaciones con el equipo, verificar los acuerdos y la Definition of Done, reproducir pruebas y conciliar las métricas.'),
('17–18. Referencias y cierre (15 s)','Mostramos las referencias utilizadas, entre ellas la Guía Scrum y Kniberg. Muchas gracias por su atención. Quedamos atentos a sus preguntas.')])]
md=['# Guion de exposición · Sol de Shirá · Sprint 1','Duración: 15 minutos, cinco integrantes con tres minutos cada uno. Las pausas, cambios de diapositiva y demostración forman parte del tiempo. Antonella tiene la introducción y el problema, sin arquitectura ni métricas.','Resultado: evaluación documental preliminar, no consensuada. 20/64 = 31,25 %, mostrado con un decimal: 31,3 %. No atribuir a las fotos eventos o acuerdos que no muestran.']
pdfmetrics.registerFont(TTFont('ArialTask','C:/Windows/Fonts/arial.ttf'));pdfmetrics.registerFont(TTFont('ArialTaskBold','C:/Windows/Fonts/arialbd.ttf'))
styles=getSampleStyleSheet();styles.add(ParagraphStyle(name='TaskTitle',fontName='ArialTaskBold',fontSize=17,leading=21,spaceAfter=10));styles.add(ParagraphStyle(name='TaskSub',fontName='ArialTaskBold',fontSize=11,leading=15,spaceBefore=9,spaceAfter=5));styles.add(ParagraphStyle(name='TaskBody',fontName='ArialTask',fontSize=10.5,leading=14.5,spaceAfter=7))
story=[]
for j,(person,scope,sections) in enumerate(parts):
 if j:story.append(PageBreak())
 story+=[Paragraph(person,styles['TaskTitle']),Paragraph(escape(scope),styles['TaskSub'])]
 md+=['',f'## {person}',scope]
 for h,body in sections:
  story.append(Paragraph(escape(h),styles['TaskSub']));md+=['',f'### {h}',body]
  for paragraph in body.split('\n\n'):story.append(Paragraph(escape(paragraph),styles['TaskBody']))
(B/'final/Guion_15_minutos_5_integrantes.md').write_text('\n\n'.join(md),encoding='utf-8')
def footer(canvas,doc):
 canvas.setFont('ArialTask',8);canvas.drawString(45,25,'Sol de Shirá · Sprint 1 · Guion de 15 minutos · Evaluación documental preliminar');canvas.drawRightString(A4[0]-45,25,str(doc.page))
SimpleDocTemplate(str(B/'final/Guion_15_minutos_5_integrantes.pdf'),pagesize=A4,rightMargin=45,leftMargin=45,topMargin=38,bottomMargin=42).build(story,onFirstPage=footer,onLaterPages=footer)
# Embed the same speaking script into notes of each part's first slide.
p=B/'build/candidate.pptx'
with zipfile.ZipFile(p) as z:f={k:z.read(k) for k in z.namelist()}
ns={'p':'http://schemas.openxmlformats.org/presentationml/2006/main','a':'http://schemas.openxmlformats.org/drawingml/2006/main'}
for sn,(person,scope,sections) in zip([1,5,7,8,11],parts):
 k=f'ppt/notesSlides/notesSlide{sn}.xml';r=E.fromstring(f[k])
 for sp in r.findall('.//p:sp',ns):
  ph=sp.find('p:nvSpPr/p:nvPr/p:ph',ns)
  if ph is not None and ph.get('type')=='body':
   tx=sp.find('p:txBody',ns)
   for pp in tx.findall('a:p',ns):tx.remove(pp)
   for txt in [person,scope]+[h+'\n'+b for h,b in sections]:
    pp=E.SubElement(tx,'{'+ns['a']+'}p');rr=E.SubElement(pp,'{'+ns['a']+'}r');E.SubElement(rr,'{'+ns['a']+'}t').text=txt
 f[k]=E.tostring(r,xml_declaration=True,encoding='UTF-8',standalone=True)
with zipfile.ZipFile(p,'w',zipfile.ZIP_DEFLATED) as z:
 for k,v in f.items():z.writestr(k,v)
print('Guion PDF y Markdown generados.')
