import fs from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {Workbook, SpreadsheetFile} from 'file:///C:/Users/tania/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs';
const dir = fileURLToPath(new URL('.', import.meta.url));
const wb=Workbook.create();
const sh=wb.worksheets.add('Días 3 al 6');
const col=n=>String.fromCharCode(65+n);
sh.getRange('A1:Q56').format.font={name:'Arial',size:10};
sh.getRange('A1:Q56').format.rowHeight=21;
const days=[
 {day:3,date:2,hu:'HU4',first:22,last:28,doing:[22,25,26],done:[23,24]},
 {day:4,date:3,hu:'HU5',first:29,last:33,doing:[29,31,32],done:[30]},
 {day:5,date:4,hu:'HU6',first:34,last:38,doing:[34,36,37],done:[35]},
 {day:6,date:5,hu:'HU7',first:39,last:43,doing:[39,41,42],done:[40]}
];
for(let c=0;c<17;c++) sh.getRange(`${col(c)}1:${col(c)}56`).format.columnWidth= c%6===0?13:c%6===1?25:c%6===5?4:15;
for(const [i,d] of days.entries()){
 const r=1+i*14;
 const ids=Array.from({length:d.last-d.first+1},(_,j)=>d.first+j);
 for(let t=0;t<3;t++){
  const c=t*6;
  sh.getCell(r-1,c).values=[[`Día ${d.day}`]];
  sh.getCell(r-1,c+1).values=[[`Objetivo: ${d.hu}`]];
  sh.getRange(`${col(c)}${r}:${col(c+4)}${r}`).format.font.bold=true;
  sh.getCell(r,c).values=[['Fecha y Hora']];
  sh.getCell(r,c+1).values=[[new Date(Date.UTC(2026,10,d.date,[8,12,23][t]))]];
  sh.getCell(r,c+1).setNumberFormat('dd-mm-yyyy hh:mm AM/PM');
  sh.getRange(`${col(c+2)}${r+1}:${col(c+4)}${r+1}`).values=[['Por hacer','Haciendo','Hecho']];
  const head=sh.getRange(`${col(c+2)}${r+1}:${col(c+4)}${r+1}`);
  head.format.fill='#FF9900';head.format.horizontalAlignment='center';
  const matrix=ids.map(id=>{
   const state=t===0?0:t===2?2:d.done.includes(id)?2:d.doing.includes(id)?1:0;
   const row=['','','']; row[state]=`T${String(id).padStart(2,'0')}`;return row;
  });
  if(new Set(matrix.flat().filter(Boolean)).size!==ids.length)throw Error('Tareas duplicadas');
  sh.getRange(`${col(c+2)}${r+2}:${col(c+4)}${r+1+ids.length}`).values=matrix;
  sh.getRange(`${col(c+2)}${r+2}:${col(c+4)}${r+11}`).format.horizontalAlignment='center';
  sh.getRange(`${col(c)}${r}:${col(c+4)}${r+11}`).format.borders={preset:'all',style:'thin',color:'#E0E0E0'};
 }
}
sh.mergeCells('A57:Q57');sh.getRange('A57').values=[['Avance ficticio. Días 1 y 2: HU22, HU1, HU2 y HU3. T22 corresponde a HU4.']];
sh.getRange('A57:Q57').format.font={name:'Arial',size:10,italic:true,color:'#666666'};
wb.recalculate();
console.log((await wb.inspect({kind:'table',range:"'Días 3 al 6'!A1:Q5",tableMaxRows:5,tableMaxCols:17,maxChars:2500})).ndjson);
const preview=await wb.render({sheetName:'Días 3 al 6',range:'A1:Q57',scale:1,format:'png'});
await fs.writeFile(`${dir}/preview.png`,new Uint8Array(await preview.arrayBuffer()));
await (await SpreadsheetFile.exportXlsx(wb)).save(`${dir}/Tablero_dias_3_al_6.xlsx`);
console.log('Exportación completada');
