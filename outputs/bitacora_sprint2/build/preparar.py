import json,datetime
from pathlib import Path
B=Path('outputs/bitacora_sprint2/build');items=json.loads((B/'tareas_fuente.json').read_text(encoding='utf-8'))
people={'Carlos':'Carlos Gonzales','Julio':'Julio Pantoja','Miguel':'Miguel Acevedo','Lee':'Lee Orbezo','Antonella':'Antonella Baldeon'}
cat={int(x['task'][1:]):x for x in items if x['row']>=63}
assert set(cat)==set(range(1,32))
actions={1:'Diseño del listado y los formularios de administración de cabañas en Angular.',2:'Modelado de secuencia de creación, edición y deshabilitación de cabañas.',3:'Desarrollo de endpoints y permisos para la gestión administrativa de cabañas.',4:'Integración de los servicios HTTP del CRUD de cabañas con el gateway.',5:'Registro de la revisión funcional y pruebas de gestión y deshabilitación lógica de cabañas.',6:'Diseño del modal de check-out y de los campos condicionales de daños.',7:'Modelado de secuencia del check-out y liquidación final.',8:'Desarrollo del procesamiento de salida, cargos y cambio de estados de la reserva.',9:'Integración del check-out desde Angular con el backend.',10:'Registro de pruebas de salidas con y sin cargos y cambios de estado.',11:'Diseño de la vista del historial personal de reservas del cliente.',12:'Modelado de secuencia de consulta del historial por usuario autenticado.',13:'Desarrollo de la consulta de reservas filtrada por la identidad del cliente.',14:'Integración HTTP del historial de reservas en Angular.',15:'Registro de pruebas del historial, filtros por usuario y adaptación visual.',16:'Diseño del formulario de estrellas, comentario y listado público de reseñas.',17:'Modelado de secuencia de publicación y consulta de reseñas.',18:'Modelado de la tabla de reseñas y de la restricción de una reseña por reserva.',19:'Desarrollo de endpoints y validaciones de publicación de reseñas.',20:'Integración de reseñas en Angular a través del gateway.',21:'Registro de pruebas de reseñas, duplicados y consulta pública.',22:'Diseño del dashboard con gráficos de ingresos por periodo y ranking de cabañas.',23:'Modelado de secuencia de consulta y agregación de métricas.',24:'Desarrollo de endpoints analíticos y filtros por fechas para métricas.',25:'Integración de las APIs analíticas con los gráficos del dashboard.',26:'Registro de pruebas de cálculo y filtros del dashboard, según la tarea planificada.',27:'Diseño del formulario público de registro y sus validaciones visuales.',28:'Modelado de secuencia del registro entre Angular, gateway, autenticación y usuarios.',29:'Desarrollo del endpoint público de registro, hash de contraseña y rol CLIENTE.',30:'Integración HTTP del registro y redirección al inicio de sesión.',31:'Registro de pruebas de correo duplicado, validaciones y funcionamiento del formulario.'}
# Exact snapshots from the user's eight-day board. P/H/F = Por hacer/Haciendo/Hecho.
spec=[
(1,'2026-11-06','HU8',[(i,'P','F' if i<=2 else 'H' if i==3 else 'P','F') for i in range(1,6)]),
(2,'2026-11-08','HU9',[(i,'P','H' if i<=8 else 'P','F' if i<=9 else 'P') for i in range(6,11)]),
(3,'2026-11-09','HU10',[(i,'P','F' if i<=12 else 'H' if i==13 else 'P','F' if i<=13 else 'P') for i in range(10,16)]),
(4,'2026-11-10','HU11',[(i,'P','F' if i<=16 else 'H' if i<=18 else 'P','F' if i<=18 else 'P') for i in range(14,22)]),
(5,'2026-11-11','HU11',[(i,'P','H' if i==19 else 'P','F') for i in range(19,22)]),
(6,'2026-11-15','HU19',[(i,'P','F' if i<=23 else 'H' if i==24 else 'P','F' if i<=24 else 'H' if i==25 else 'P') for i in range(22,27)]),
(7,'2026-11-16','HU19 y HU22',[(i,'H' if i==25 else 'P','H' if i in [25,27,28] else 'P','F' if i<=28 else 'P') for i in range(25,32)]),
(8,'2026-11-17','HU22',[(i,'P','H' if i==29 else 'P','F') for i in range(29,32)])]
state={'P':'Por hacer','H':'Haciendo','F':'Hecho'};raw=[];bit=[]
for day,date,obj,tasks in spec:
 for num,s8,s12,s23 in tasks:
  c=cat[num];task=f'T{num:02}';name=people[c['person']]
  label=c['title'].strip()
  if num==29:label='Desarrollo Backend (Registro de clientes)'
  note=f'A las 08:00: {state[s8].lower()}; a las 12:00: {state[s12].lower()}; a las 23:00: {state[s23].lower()}.'
  if day==3 and num==10:note+=' Continuación de HU9 pendiente del 08/11.'
  if day==4 and num in [14,15]:note+=' Continuación de HU10 pendiente del 09/11.'
  if day==7 and num in [25,26]:note+=' Continuación de HU19 del 15/11.'
  if day==8:note+=' Continuación de HU22 del 16/11.'
  activity=('Pendiente de iniciar: ' if s8==s12==s23=='P' else '')+actions[num]
  bit.append([f'Día {day}',date,name,'Integrante del equipo','08:00 / 12:00 / 23:00',activity+' '+note,c['hu']+' · '+task+' · '+label,state[s23],'Reconstrucción del tablero; responsable sugerido en el Excel.'])
  raw.append([f'Día {day}',date,obj,c['hu'],task,state[s8],state[s12],state[s23],label,name])
assert len(bit)==42
last={}
for row in raw:last[row[4]]=row[7]
assert len(last)==31 and set(last.values())=={'Hecho'}
inc=[
['INC-S2-01','2026-11-08','T10 (HU9) permanece Por hacer al cierre; la tarea de pruebas se arrastra al día 3. La causa del pendiente no está registrada.',people['Antonella'],'Integrante del equipo','HU9 · T10 · Pruebas de check-out','Continuar el registro de pruebas el 09/11. El tablero la muestra Hecho desde las 12:00; confirmar resultados y evidencia de ejecución.','Tablero: día 2, 23:00 y día 3, 12:00.','Pendiente observado; causa por confirmar','Cierre de tarea registrado el 09/11'],
['INC-S2-02','2026-11-09','T14 y T15 (HU10) siguen Por hacer a las 23:00 y pasan al día 4. El historial aún tiene integración y pruebas pendientes según el tablero.',people['Carlos']+' / '+people['Miguel'],'Integrantes del equipo','HU10 · T14–T15 · Historial del cliente','Retomar integración y registro de pruebas el 10/11. Ambas tareas figuran Hecho a las 12:00; confirmar evidencia técnica.','Tablero: día 3, 23:00 y día 4, 12:00.','Arrastre observado; motivo no documentado','Cierre de tareas registrado el 10/11'],
['INC-S2-03','2026-11-10','T19, T20 y T21 (HU11) permanecen Por hacer al cierre. La parte de backend, integración y pruebas de reseñas continúa el día 5.',people['Carlos']+' / '+people['Lee']+' / '+people['Miguel'],'Integrantes del equipo','HU11 · T19–T21 · Reseñas','Continuar el 11/11: T19 pasa a Haciendo al mediodía y las tres figuran Hecho a las 23:00. Registrar pruebas de aceptación por separado.','Tablero: días 4 y 5.','Trabajo pendiente observado; sin diagnóstico de fallo','Cierre de tareas registrado el 11/11'],
['INC-S2-04','2026-11-15','T25 (HU19) sigue Haciendo a las 23:00 y T26 permanece Por hacer. El dashboard necesita continuidad de integración y pruebas.',people['Carlos']+' / '+people['Lee'],'Integrantes del equipo','HU19 · T25–T26 · Dashboard','Dar continuidad el 16/11 y registrar validaciones de cálculo y filtros. T25 y T26 figuran Hecho en el corte de las 23:00.','Tablero: día 6, 23:00 y día 7, 23:00.','Pendientes observados; dependencia técnica no demostrada','Cierre de tareas registrado el 16/11'],
['INC-S2-05','2026-11-16','T29, T30 y T31 (HU22) quedan Por hacer al cierre, aunque T27 y T28 figuran Hecho. El registro de clientes continúa el día 8.',people['Antonella'],'Integrante del equipo','HU22 · T29–T31 · Registro público','Continuar backend, integración y registro de pruebas el 17/11. T29 figura Haciendo a las 12:00 y las tres Hecho a las 23:00; confirmar aceptación.','Tablero: día 7, 23:00 y día 8.','Arrastre observado; sin causa técnica registrada','Cierre de tareas registrado el 17/11'],
['INC-S2-06','2026-11-17','El título de T29 en el Excel dice «Consultas y Filtros Avanzados», pero su descripción corresponde al registro de clientes.',people['Antonella'],'Integrante del equipo','HU22 · T29 · Tareas de HU, fila 91','Proponer el título «Desarrollo Backend (Registro de clientes)». La bitácora usa esa denominación; el archivo fuente se conserva intacto.','Historias de usuario.xlsx · Tareas de HU!E91:F91.','Inconsistencia documental comprobada','Corrección de nombre propuesta'],
['INC-S2-07','2026-11-15','El tablero sitúa HU19 en el 15–16/11, mientras existen commits de métricas fechados el 10/11. Las fechas de seguimiento no demuestran el inicio real de implementación.',people['Carlos']+' / '+people['Lee'],'Integrantes del equipo','HU19 · Tablero y versión de Git','Conservar las fechas del tablero para esta bitácora y documentar por separado las fechas de implementación, integración y revisión. Confirmar el desfase con el equipo.','Tablero días 6–7; commits 4c25363 y a46233b, 10/11/2026.','Diferencia documental comprobada','Conciliación de fechas pendiente']]
# The field keeps Sprint 1's seven core incident columns, adding traceability and status.
payload={'raw':raw,'bitacora':bit,'incidencias':inc,'periodo':'06 al 17 de noviembre de 2026','days':8,'records':42,'tasks':31,'source':'Tablero de Sprint 2 pegado por el usuario (8 días, cortes 08:00/12:00/23:00). Historias de usuario.xlsx: Tareas de HU, filas 63–93 (nombres, descripciones y responsables sugeridos); Bitacora Sprint 1, filas 2–3 (estructura). Git local para la observación documental INC-S2-07.'}
(B/'datos.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8')
print('42 registros, 31 tareas, 8 días y 7 incidencias/observaciones.')
