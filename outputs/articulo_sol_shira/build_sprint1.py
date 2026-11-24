from pathlib import Path

base = Path(__file__).resolve().parent
source = (base / 'build_article.py').read_text(encoding='utf-8-sig')
source = source.replace("out=root/'outputs'/'articulo_sol_shira'", "out=root/'outputs'/'articulo_sprint1'")
source = source.replace("fontpath='C:/Windows/Fonts/arial.ttf'", "exec(Path(r'" + str(base / 'sprint1_overrides.py') + "').read_text(encoding='utf-8'), globals())\nfontpath='C:/Windows/Fonts/arial.ttf'")
source = source.replace("for index,name in [(1,'scrum.png'),(2,'c4.png')]:", "exec(Path(r'" + str(base / 'diagramas_sprint1.py') + "').read_text(encoding='utf-8'), globals())\nfor index,name in [(1,'scrum.png'),(2,'c4.png')]:")
requirements = '''
requirements = requirements[:10] + [
 ('RNF01','Seguridad','Aplicar hash BCrypt al almacenamiento de contraseñas.','Sistema','HU21 (autenticación)','1','SecurityConfig; PasswordEncoder','Verificación sobre versión histórica pendiente','Código histórico presente'),
 ('RNF02','Usabilidad','Permitir usar catálogo, reserva y pago desde navegador con pantalla reducida.','Cliente','HU1–HU7','1','Vistas y estilos Angular','Verificación multidispositivo pendiente','Criterio por validar'),
 ('RNF03','Integridad','Controlar reintentos del pago restante mediante una clave de idempotencia única.','Sistema','HU7','1','PagoRestante; claveIdempotencia','PagoRestanteTest','Código histórico y prueba disponible'),
 ('RNF04','Verificabilidad','Disponer de pruebas reproducibles del flujo reserva, pago y check-in.','Equipo','HU3–HU7','1','Clases de prueba en 776a2ec','Ejecución integral por reproducir','Criterio por validar')
]
historical = {
 'RF01': ('AuthController; AuthServiceImpl','Autenticación por credenciales; ejecución por reproducir'),
 'RF02': ('JwtAuthenticationFilter','JwtPanelReservasTest'),
 'RF03': ('CabanaController','CabanaResumenTest'),
 'RF04': ('CabanaController','Consulta de detalle por reproducir'),
 'RF05': ('ReservaController; OcupacionInternaController','OcupacionTest'),
 'RF06': ('ReservaController; ReservaServiceImpl','Creación y superposición por reproducir'),
 'RF07': ('PagoController; WebhookController','OrdersFlowTests'),
 'RF08': ('ReservaController','ReservaConsultaControllerTest'),
 'RF09': ('ReservaController','ReservaDetalleControllerTest'),
 'RF10': ('PagoRestanteController; ReservaPagoInternoController','PagoRestanteTest; CheckInTest')
}
clean=[]
for req in requirements:
 values=list(req)
 if req[0] in historical:
  values[6],values[7]=historical[req[0]]
  values[8]='Código presente en 776a2ec; aceptación por validar'
 clean.append(tuple(values))
requirements=clean
'''
source = source.replace("(out/'matriz_datos.json').write_text", requirements + "\n(out/'matriz_datos.json').write_text")
source = source.replace('16 RF y 5 RNF', '10 RF y 4 RNF')
source = source.replace("output=out/'Articulo_Corto_Sol_de_Shira.docx'", """
# Preserve only the Sprint 1 row and the chart slot from the original table.
for index in (3,2):
    row = tables[4].rows[index]
    tables[4]._tbl.remove(row._tr)
# Keep all inserted pictures attached to their captions.
for paragraph in doc.paragraphs:
    if paragraph._p.xpath('.//w:drawing'):
        paragraph.paragraph_format.keep_with_next = True
# Preserve the template distinction between a bold label and normal body text.
body_properties = deepcopy(paras[18].runs[0]._r.rPr)
for index, label, text in [(9, 'Resumen— ', abstract), (11, 'Abstract— ', eng)]:
    paragraph = paras[index]
    replace(paragraph, '')
    lead = paragraph.add_run(label)
    lead.bold = True
    run = paragraph.add_run(text)
    if body_properties is not None:
        run._r.insert(0, deepcopy(body_properties))
    run.bold = False
for paragraph in doc.paragraphs:
    if paragraph.text.startswith(('La columna moda asigna', 'El tablero y la bitácora ofrecen')):
        for run in paragraph.runs:
            if run._r.rPr is not None:
                run._r.remove(run._r.rPr)
            if body_properties is not None:
                run._r.insert(0, deepcopy(body_properties))
    if paragraph.text.startswith(('Contribución de autores.', 'Uso de IA generativa.', 'Disponibilidad de código y datos.', 'Conflicto de intereses.', 'Agradecimientos.')):
        text = paragraph.text
        label, remainder = text.split('. ', 1)
        replace(paragraph, '')
        paragraph.add_run(label + '. ').bold = True
        run = paragraph.add_run(remainder)
        if body_properties is not None:
            run._r.insert(0, deepcopy(body_properties))
        run.bold = False
all_text = '\\n'.join(paragraph.text for paragraph in doc.paragraphs)
assert 'Sprint 2' not in all_text and '84 ejecuciones' not in all_text
assert len(requirements) == 14
output=out/'Articulo_Corto_Sol_de_Shira_Sprint_1_Revisado.docx'
""")
exec(compile(source, str(base / 'build_article.py'), 'exec'), globals())
