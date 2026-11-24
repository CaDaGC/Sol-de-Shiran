import {finalizePresentation} from 'file:///C:/Users/tania/.codex/plugins/cache/openai-primary-runtime/presentations/26.1116.12020/skills/presentations/container_tools/artifact_tool_utils.mjs';
const root='C:/Users/tania/Documents/Tareas de DAD/Sol-de-Shiran/outputs/presentacion_final';
const skill='C:/Users/tania/.codex/plugins/cache/openai-primary-runtime/presentations/26.1116.12020/skills/presentations';
console.log(await finalizePresentation({workspaceDir:root,candidatePath:root+'/build/candidate.pptx',finalPath:root+'/final/Presentacion_Sprint_1_Resultados_y_Evidencias.pptx',pythonExecutable:'C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1116-1220-0_26.1116.1220.0_x64__3k8sg7r9htsxt/dependencies/python/python.exe',integrityValidatorPath:skill+'/container_tools/inspect_presentation_package_integrity.py',layoutValidatorPath:skill+'/container_tools/inspect_presentation_layout_geometry.py',explicitTotalSlideCount:18,layoutArgs:["--require-native-table-slide","5","--require-native-table-slide","7","--require-native-table-slide","9","--require-native-table-slide","13"],requiredNativeTableOwnerSlides:[5,7,9,13],verifyArtifactToolImport:true,receiptPath:root+'/build/validation.json'}));



