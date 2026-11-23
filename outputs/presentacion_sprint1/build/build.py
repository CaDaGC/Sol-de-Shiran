import zipfile,copy,re
from pathlib import Path
from lxml import etree as E
from PIL import Image
base=Path('outputs/presentacion_sprint1'); src=Path('C:/Users/tania/Downloads/Plantilla_Exposicion_Articulo_Corto_IS1.pptx')
n={'a':'http://schemas.openxmlformats.org/drawingml/2006/main','p':'http://schemas.openxmlformats.org/presentationml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'}
def tag(k):return '{'+n[k.split(':')[0]]+'}'+k.split(':')[1]
D={
1:{12:'Evaluación del proceso de desarrollo de software del sistema de gestión de reservas basado en Scrum mediante listas de chequeo para el Complejo Turístico Sol de Shirá',1000:'ARTÍCULO CORTO · EVALUACIÓN DEL SPRINT 1',1001:'Complejo Turístico Sol de Shirá\nTrujillo, Perú · Sprint 1 · Sustentación: 26/11/2026'},
2:{14:'Julio Pantoja\nProduct Owner',29:'Carlos Gonzales\nScrum Master',30:'Miguel Acevedo · Análisis y pruebas\nLee Orbezo · Desarrollo\nAntonella Baldeon · Base de datos\nDevelopers',1000:'PO',1001:'SM',1002:'DEV',1003:'Ingeniería de Software I · Evaluación del Sprint 1'},
3:{25:'Problema y antecedentes · 2 min',29:'Método y evaluación de Scrum · 3 min',33:'Evidencias y demostración · 4 min',37:'Discusión y conclusiones · 1 min'},
4:{32:'Objetivo: evaluar la aplicación de Scrum en el Sprint 1 mediante listas de chequeo sustentadas en evidencias del proyecto.',34:'Organización\nComplejo Turístico Sol de Shirá, Trujillo.\nHospedaje y recreación. Reservas mediante WhatsApp y registros manuales. Adelanto obligatorio del 50 %.',35:'Problema y contribución\nInformación dispersa y riesgo de cruce de reservas.\nEl cierre de tareas requiere contrastarse con pruebas y DoD [1].\nAporte: evaluación documental y trazabilidad del Sprint 1.'},
5:{1002:'Síntesis: la evidencia y los acuerdos permiten evaluar las adaptaciones de Scrum. Nuestro caso revisa el primer incremento de reservas.'},
6:{1002:'Objetivo: digitalizar la gestión de reservas.\nSprint 1: catálogo, reserva, adelanto y check-in.\nEntregables: web, API, datos y pruebas.\nEquipo: PO, SM y Developers.\nRiesgos: pasarela e integración de pagos.',1004:'Matriz: 10 RF y 4 RNF del Sprint 1.\nTrazabilidad: requisito, HU y evidencia de código.\nFuente: descripción del negocio y HU.\nCambios: registrar motivo y decisión.\nCorte técnico: 776a2ec, 05/11/2026.'},
7:{1001:'Evaluación con listas de chequeo\nCada criterio requiere evidencia observable [1], [2].',1005:'Roles y alcance\nPO: Julio Pantoja. SM: Carlos Gonzales.\nSprint 1: HU1–HU7 y autenticación.\nTablero: 31/10–04/11/2026.',1009:'Siete ámbitos\nBacklog, planificación y Sprint Backlog, ejecución, Daily, Review, retrospectiva e incremento.',1013:'Escala y evidencias\nCumple: 2. Parcial: 1. No cumple: 0.\nEvidencias: HU, tablero, bitácora, código y pruebas. Sin evidencia: 0.'},
8:{1001:'',1002:'Estilo: microservicios con API Gateway.\nDecisiones: JWT, BCrypt y MySQL por dominio.\nVersión: commit 776a2ec del 05/11/2026.\nPruebas: cinco escenarios trazados. Ejecución histórica por reproducir.'},
9:{},
10:{1001:'42 pts\nEstimación nominal seleccionada',1002:'8 HU\nSeleccionadas, cierre por validar',1003:'Pendiente\nTasa de pruebas históricas',1004:'Entrevista\nValidación de versión pendiente',1005:'',1007:'Interpretación: el burndown registra 43 unidades iniciales. La selección suma 42 puntos. Falta conciliar ambas medidas antes de calcular velocidad.'},
11:{1001:'Acceso\nCliente y trabajador según rol',1005:'Catálogo y disponibilidad\nSeleccionar cabaña y fechas',1009:'Reserva y adelanto\nRegistrar solicitud y pagar el 50 %',1013:'Ingreso presencial\nConsultar reserva, cobrar saldo y check-in',1017:'Tablero en Google Sheets\nMostrar HU y evolución del Sprint 1',1021:'Evaluación del proceso\nContrastar un criterio con su evidencia',1030:'Versión de demostración: Sprint 1, commit 776a2ec. Respaldo: capturas del flujo y evidencias del tablero.'},
12:{1004:'Trazabilidad: 10 RF y 4 RNF permiten relacionar el alcance del Sprint 1 con historias y código.',1008:'Evaluación: siete ámbitos y escala 2–1–0. El porcentaje requiere ítems consensuados y evidencias revisadas.',1012:'Limitaciones: diferencia entre puntos y burndown, acuerdos de eventos por verificar y aceptación de la versión pendiente.',1016:'Siguiente paso: reproducir 776a2ec, ejecutar pruebas, conciliar el burndown y aplicar las listas con el equipo.'},
13:{7:'[1] K. Schwaber y J. Sutherland, The Scrum Guide, 2020. scrumguides.org\n\n[2] H. Kniberg, Scrum and XP from the Trenches, 2.ª ed. InfoQ, 2015.\n\n[3] A. Alami y O. Krancher, “How Scrum adds value to achieving software quality?”, Empirical Software Engineering, vol. 27, art. 165, 2022. doi: 10.1007/s10664-022-10208-4.\n\n[4] M. Hron y N. Obwegeser, “Why and how is Scrum being adapted in practice: A systematic review”, J. Syst. Softw., vol. 183, 2022. doi: 10.1016/j.jss.2021.111110.'}}
D[2][1002]='Developers\nMiguel Acevedo\nLee Orbezo\nAntonella Baldeon'
D[2][30]=''
def fill(tx,text,sz=None):
 ps=tx.findall('a:p',n); proto=ps[0] if ps else E.Element(tag('a:p'))
 rp=tx.find('.//a:rPr',n)
 rp=copy.deepcopy(rp) if rp is not None else E.Element(tag('a:rPr'))
 if sz:rp.set('sz',str(sz))
 for p in ps:tx.remove(p)
 for line in text.split('\n'):
  p=E.SubElement(tx,tag('a:p'));pp=proto.find('a:pPr',n)
  if pp is not None:p.append(copy.deepcopy(pp))
  r=E.SubElement(p,tag('a:r'));r.append(copy.deepcopy(rp));E.SubElement(r,tag('a:t')).text=line
files={}
with zipfile.ZipFile(src) as z:files={k:z.read(k) for k in z.namelist()}
for i in range(1,15):
 path=f'ppt/slides/slide{i}.xml';r=E.fromstring(files[path])
 for sp in r.findall('.//p:sp',n):
  sid=int(sp.find('p:nvSpPr/p:cNvPr',n).get('id'))
  if sid in D.get(i,{}):
   size=3200 if (i,sid)==(1,12) else (1800 if (i,sid)==(2,30) else None)
   fill(sp.find('p:txBody',n),D[i][sid],size)
 # native table replacements
 tables=r.findall('.//a:tbl',n)
 if i==5:
  vals=[['Estudio','Contexto','Enfoque','Hallazgo','Aplicación'],['Alami y Krancher [3]','39 entrevistas y 2 casos','Calidad en Scrum','Colaboración y verificación','Contrastar tareas con pruebas'],['Hron y Obwegeser [4]','Revisión sistemática','Adaptación de Scrum','Explicitar las adaptaciones','Documentar acuerdos'],['Schwaber y Sutherland [1]','Marco Scrum','Inspección y adaptación','Propósitos de los eventos','Criterios verificables'],['Kniberg [2]','Experiencia de equipos','Scrum y XP','Prácticas contextualizadas','Orientar la revisión del tablero']]
  for row,vs in zip(tables[0].findall('a:tr',n),vals):
   for cell,v in zip(row.findall('a:tc',n),vs):fill(cell.find('a:txBody',n),v)
 if i==9:
  for row,v in zip(tables[0].findall('a:tr',n)[1:],['Evaluar solo el Sprint 1','Usar disciplinas para organizar evidencias','Especialidades dentro de Developers','Vincular documentación con el incremento']):fill(row.findall('a:tc',n)[-1].find('a:txBody',n),v)
 if i==10:
  tb=tables[0];rows=tb.findall('a:tr',n)
  vals=[['Sprint','Alcance documentado','Pts. nom.','Cierre'],['1','Catálogo, reserva, adelanto y check-in','42','Por validar']]
  for row,vs in zip(rows,vals):
   for cell,v in zip(row.findall('a:tc',n),vs):fill(cell.find('a:txBody',n),v)
  for row in rows[2:]:tb.remove(row)
 if i==2:
  for sp in r.findall('.//p:sp',n):
   if sp.find('p:nvSpPr/p:cNvPr',n).get('id')=='1002':
    for rp in sp.findall('.//a:rPr',n):rp.set('sz','1800')
 if i==10:
  for sp in r.findall('.//p:sp',n):
   if sp.find('p:nvSpPr/p:cNvPr',n).get('id') in ['1001','1002','1003','1004']:
    for pi,pp in enumerate(sp.findall('p:txBody/a:p',n)):
     for rp in pp.findall('.//a:rPr',n):rp.set('sz','3200' if pi==0 else '1800')
 files[path]=E.tostring(r,xml_declaration=True,encoding='UTF-8',standalone=True)
# Insert existing article figures into the designated image frames, preserving the template.
for i,sid,img in [(8,1001,'outputs/articulo_sprint1/c4.png'),(10,1005,'outputs/articulo_sprint1/burndown.png')]:
 path=f'ppt/slides/slide{i}.xml';r=E.fromstring(files[path]);sp=next(s for s in r.findall('.//p:sp',n) if s.find('p:nvSpPr/p:cNvPr',n).get('id')==str(sid))
 xf=sp.find('p:spPr/a:xfrm',n);off=xf.find('a:off',n);ext=xf.find('a:ext',n);x,y=int(off.get('x')),int(off.get('y'));w,h=int(ext.get('cx')),int(ext.get('cy'))
 iw,ih=Image.open(img).size; factor=min((w-150000)/iw,(h-150000)/ih);nw,nh=int(iw*factor),int(ih*factor)
 relpath=f'ppt/slides/_rels/slide{i}.xml.rels';rel=E.fromstring(files[relpath]);nsrel='http://schemas.openxmlformats.org/package/2006/relationships';rid='rIdFigureSprint1';E.SubElement(rel,'{'+nsrel+'}Relationship',Id=rid,Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/image',Target=f'../media/sprint1-{i}.png')
 files[relpath]=E.tostring(rel,xml_declaration=True,encoding='UTF-8',standalone=True);files[f'ppt/media/sprint1-{i}.png']=Path(img).read_bytes()
 pic=E.fromstring(f'''<p:pic xmlns:p="{n['p']}" xmlns:a="{n['a']}" xmlns:r="{n['r']}"><p:nvPicPr><p:cNvPr id="2000" name="Figura del artículo Sprint 1"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="{x+(w-nw)//2}" y="{y+(h-nh)//2}"/><a:ext cx="{nw}" cy="{nh}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic>''');r.find('p:cSld/p:spTree',n).append(pic);files[path]=E.tostring(r,xml_declaration=True,encoding='UTF-8',standalone=True)
# Speaker notes cite project evidence and disclose the scope of results.
for i in range(1,15):
 k=f'ppt/notesSlides/notesSlide{i}.xml'
 if k in files:
  r=E.fromstring(files[k]);b=next((s for s in r.findall('.//p:sp',n) if s.find('p:nvSpPr/p:nvPr/p:ph',n) is not None and s.find('p:nvSpPr/p:nvPr/p:ph',n).get('type')=='body'),None)
  if b is not None:fill(b.find('p:txBody',n),'Fuente: artículo del Sprint 1, Historias de usuario.xlsx y repositorio CaDaGC/Sol-de-Shiran, corte 776a2ec (05/11/2026). Alcance: HU1–HU7 y autenticación. Las listas no tienen puntuaciones verificadas. El cierre de HU, las pruebas históricas y la aceptación empresarial requieren confirmación. Referencias bibliográficas en diapositiva 13.');files[k]=E.tostring(r,xml_declaration=True,encoding='UTF-8',standalone=True)
ct=E.fromstring(files['[Content_Types].xml'])
E.SubElement(ct,'{http://schemas.openxmlformats.org/package/2006/content-types}Default',Extension='png',ContentType='image/png')
files['[Content_Types].xml']=E.tostring(ct,xml_declaration=True,encoding='UTF-8',standalone=True)
out=base/'build/candidate.pptx'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for k,v in files.items():z.writestr(k,v)
print(out)
