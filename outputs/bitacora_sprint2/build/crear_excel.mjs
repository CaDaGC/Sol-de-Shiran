import fs from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {Workbook,SpreadsheetFile} from 'file:///C:/Program%20Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1118-904-0_26.1118.904.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs';
const base=new URL('../',import.meta.url);const data=JSON.parse(await fs.readFile(new URL('datos.json',import.meta.url),'utf8'));
const wb=Workbook.create();const log=wb.worksheets.add('Bitácora Sprint 2');const inc=wb.worksheets.add('Incidencias Sprint 2');const raw=wb.worksheets.add('Tablero fuente');
const colors={dark:'#24574B',light:'#E9F2ED',ink:'#243B35',gold:'#F4E6BD'};
function baseSheet(s,last,widths,title,subtitle,notes){
 s.showGridLines=false;s.getRange(`A1:${last}60`).format.font={name:'Arial',size:11,color:colors.ink};
 for(const [row,text] of [[1,title],[2,subtitle],[3,notes[0]],[4,notes[1]]]){let r=s.getRange(`A${row}:${last}${row}`);r.merge();s.getRange(`A${row}`).values=[[text]];r.format.wrapText=true;r.format.rowHeight=row===1?34:row===2?30:38;}
 s.getRange(`A1:${last}1`).format.fill=colors.dark;s.getRange(`A1:${last}1`).format.font={name:'Arial',size:16,bold:true,color:'#FFFFFF'};
 s.getRange(`A2:${last}4`).format.fill=colors.light;
 const letters='ABCDEFGHIJKLMNOPQRSTUVWXYZ';widths.forEach((w,i)=>s.getRange(`${letters[i]}1:${letters[i]}60`).format.columnWidthPx=w);
 s.freezePanes.freezeRows(9);s.tabColor=colors.dark;
}
function table(s,headers,rows,last,height,name){
 const end=9+rows.length;s.getRange(`A9:${last}9`).values=[headers];s.getRange(`A10:${last}${end}`).values=rows;
 const t=s.tables.add(`A9:${last}${end}`,true,name);t.style='TableStyleMedium4';t.showFilterButton=true;
 s.getRange(`A9:${last}${end}`).format.wrapText=true;s.getRange(`A9:${last}${end}`).format.verticalAlignment='top';s.getRange(`A9:${last}9`).format.rowHeight=42;
 s.getRange(`A9:${last}9`).format.font={name:'Arial',bold:true,color:'#FFFFFF',size:11};s.getRange(`A9:${last}9`).format.fill=colors.dark;
 s.getRange(`A10:${last}${end}`).format.rowHeight=height;s.getRange(`B10:B${end}`).setNumberFormat('dd/mm/yyyy');
 return end;
}
baseSheet(log,'H',[85,105,175,150,180,590,250,135],'BITÁCORA DE ACTIVIDADES — SPRINT 2','Sol de Shirán · 06–17/11/2026 · 8 días de trabajo · 31 tareas y 42 registros diarios',[
 'Reconstrucción para revisión del equipo. Responsables tomados de la asignación sugerida en el Excel; no equivalen a asistencia o autoría acreditada.',
 'Hora indica los tres cortes del tablero, no horas efectivas. «Hecho» reproduce el estado registrado y no certifica aprobación de pruebas ni DoD.'
]);
baseSheet(inc,'J',[110,110,470,215,150,245,530,320,240,245],'REGISTRO DE INCIDENCIAS Y SEGUIMIENTO — SPRINT 2','Sol de Shirán · Seguimiento del 08 al 17/11/2026',[
 'Se registran pendientes observados y diferencias documentales. El tablero no identifica causas técnicas: estas quedan por confirmar.',
 'La fecha corresponde al seguimiento del tablero. Una tarea cerrada no prueba por sí sola la resolución técnica ni la aceptación de la historia.'
]);
baseSheet(raw,'J',[85,105,170,95,85,115,115,115,300,180],'TABLERO FUENTE — SPRINT 2','Transcripción de los cortes 08:00, 12:00 y 23:00 de los ocho días proporcionados',[
 'Fuente: '+data.source,
 'Las HU de T10 y T14–T15 se conservan como HU9 y HU10 aunque se arrastren a un día cuyo objetivo sea otra HU. T29 tiene un título corregido solo en este archivo.'
]);
raw.getRange('A3:J3').format.rowHeight=64;
let rawrows=data.raw.map(r=>[r[0],new Date(r[1]+'T00:00:00Z'),...r.slice(2)]);
const rawEnd=table(raw,['Día del proyecto','Fecha','Objetivo del día','HU de la tarea','Tarea','08:00','12:00','23:00','Actividad de la tarea','Responsable sugerido'],rawrows,'J',42,'TableroSprint2');
const end=table(log,['Día del proyecto','Fecha','Colaborador','Rol','Hora / cortes del tablero','Descripción de la actividad','Tarea','Estado a las 23:00'],data.bitacora.map(r=>r.slice(0,8)),'H',72,'BitacoraSprint2');
for(let i=10;i<=end;i++){
 log.getRange(`B${i}`).formulas=[[`='Tablero fuente'!B${i}`]];
 log.getRange(`C${i}`).formulas=[[`='Tablero fuente'!J${i}`]];
 log.getRange(`H${i}`).formulas=[[`='Tablero fuente'!H${i}`]];
}
const incEnd=table(inc,['ID','Fecha de seguimiento','Incidencia / observación','Persona relacionada (sugerida)','Rol','Artefacto relacionado','Solución / acción de seguimiento','Referencia','Sustento','Estado del seguimiento'],data.incidencias.map(r=>[r[0],new Date(r[1]+'T00:00:00Z'),...r.slice(2)]),'J',96,'IncidenciasSprint2');
raw.getRange(`J10:J${rawEnd}`).format.font.color='#1D4ED8';raw.getRange(`J10:J${rawEnd}`).dataValidation={rule:{type:'list',values:['Carlos Gonzales','Julio Pantoja','Miguel Acevedo','Lee Orbezo','Antonella Baldeon']}};
for(const rg of [raw.getRange(`F10:H${rawEnd}`),log.getRange(`H10:H${end}`)]){
 rg.conditionalFormats.add('containsText',{text:'Hecho',format:{fill:'#E2F0E6',font:{color:'#1D633E',bold:true}}});
 rg.conditionalFormats.add('containsText',{text:'Haciendo',format:{fill:'#FFF0C2',font:{color:'#805D11',bold:true}}});
 rg.conditionalFormats.add('containsText',{text:'Por hacer',format:{fill:'#F3F4F6',font:{color:'#525252'}}});
}
log.getRange('A6').values=[['Registros diarios']];log.getRange('B6').formulas=[[`=COUNTA(G10:G${end})`]];log.getRange('D6').values=[['Incidencias / observaciones']];log.getRange('E6').formulas=[[`=COUNTA('Incidencias Sprint 2'!A10:A${incEnd})`]];
log.getRange('A6:H6').format.rowHeight=28;log.getRange('A6:H6').format.font.bold=true;
// Mark the eight date boundaries without changing the source states.
for(let i=0;i<data.raw.length;i++)if(i===0||data.raw[i][0]!==data.raw[i-1][0]){
 for(const s of [log,raw])s.getRange(`A${i+10}:B${i+10}`).format.fill=colors.gold;
}
await wb.recalculate();
console.log((await wb.inspect({kind:'region',sheetId:'Bitácora Sprint 2',range:'A6:H6',maxChars:800,tableMaxRows:1,tableMaxCols:8})).ndjson);
console.log((await wb.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!',options:{useRegex:true,maxResults:10},maxChars:500})).ndjson);
// Verify linking behavior before delivery, then restore the source state.
const original=raw.getRange('H10').values[0][0];raw.getRange('H10').values=[['Haciendo']];await wb.recalculate();if(log.getRange('H10').values[0][0]!=='Haciendo')throw new Error('Source status link did not update');raw.getRange('H10').values=[[original]];await wb.recalculate();
await fs.mkdir(new URL('final/',base),{recursive:true});const out=await SpreadsheetFile.exportXlsx(wb);await out.save(fileURLToPath(new URL('final/Bitacora_e_Incidencias_Sprint_2.xlsx',base)));
for(const [s,range,file] of [['Bitácora Sprint 2','A9:H15','bitacora_qa.png'],['Incidencias Sprint 2','A9:J12','incidencias_qa.png'],['Tablero fuente','A9:J16','tablero_qa.png'],['Bitácora Sprint 2','A43:H51','cierre_qa.png']]){
 const img=await wb.render({sheetName:s,range,scale:1,format:'png'});await fs.writeFile(new URL(file,import.meta.url),new Uint8Array(await img.arrayBuffer()));
}
console.log('Exported 42 daily records, 7 incident/follow-up records, 3 sheets.');

