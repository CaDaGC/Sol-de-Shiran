import fs from 'node:fs/promises';
import {FileBlob,Presentation,PresentationFile} from 'file:///C:/Program%20Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1116-1220-0_26.1116.1220.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs';
const p=await PresentationFile.importPptx(await FileBlob.load(process.argv[2]));
console.log(await p.inspect({kind:'slide,layout',maxChars:1500}));
for(let i=0;i<1;i++){ const b=await p.export({slide:p.slides.items[i],format:'png',scale:0.8});await fs.writeFile(`${process.argv[3]}/slide-${i+1}.png`,new Uint8Array(await b.arrayBuffer())); }


