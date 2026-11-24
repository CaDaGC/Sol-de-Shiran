import fs from 'node:fs/promises';
import path from 'node:path';
import http from 'node:http';
import crypto from 'node:crypto';

const archivoCliente = process.argv[2];
if (!archivoCliente) {
  console.error('Uso: node docker/autorizar-drive.mjs "C:\\ruta\\cliente-oauth.json"');
  process.exit(2);
}
const configuracion = JSON.parse(await fs.readFile(path.resolve(archivoCliente), 'utf8')).installed;
if (!configuracion?.client_id || !configuracion?.client_secret) throw new Error('Selecciona las credenciales OAuth de una aplicación de escritorio.');
const destino = path.resolve('privado/google-drive.json');
try { await fs.access(destino); throw new Error('Ya existe privado/google-drive.json. Conserva una copia y retíralo antes de volver a autorizar.'); }
catch (error) { if (error.code !== 'ENOENT') throw error; }
const estado = crypto.randomBytes(32).toString('base64url');
const verificador = crypto.randomBytes(48).toString('base64url');
const desafio = crypto.createHash('sha256').update(verificador).digest('base64url');
let procesando = false;
let retorno;
const servidor = http.createServer(async (req, res) => {
  const url = new URL(req.url, retorno);
  if (url.pathname !== '/oauth2/callback') { res.writeHead(404).end('Ruta no encontrada.'); return; }
  if (req.method !== 'GET' || url.searchParams.get('state') !== estado) { res.writeHead(400).end('La autorización no coincide.'); return; }
  if (procesando) { res.writeHead(409).end('La autorización ya está en proceso.'); return; }
  procesando = true;
  try {
    if (url.searchParams.has('error') || !url.searchParams.get('code')) throw new Error('No se autorizó el almacenamiento en Drive.');
    const respuesta = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST', signal: AbortSignal.timeout(20000),
      body: new URLSearchParams({ code: url.searchParams.get('code'), client_id: configuracion.client_id,
        client_secret: configuracion.client_secret, redirect_uri: retorno, grant_type: 'authorization_code', code_verifier: verificador }),
    });
    const datos = await respuesta.json();
    if (!respuesta.ok || !datos.refresh_token) throw new Error('Google no devolvió una autorización permanente. Vuelve a autorizar con consentimiento.');
    await fs.mkdir(path.dirname(destino), { recursive: true });
    await fs.writeFile(destino, JSON.stringify({ type: 'authorized_user', client_id: configuracion.client_id,
      client_secret: configuracion.client_secret, refresh_token: datos.refresh_token }, null, 2), { flag: 'wx', mode: 0o600 });
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end('Autorización guardada localmente. Puedes cerrar esta pestaña y volver a la terminal.');
    console.log('Autorización guardada en privado/google-drive.json. No compartas este archivo ni lo subas a Git.');
    clearTimeout(limite); servidor.close();
  } catch {
    res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end('No se pudo guardar la autorización. Revisa las credenciales de escritorio y repite el proceso.');
    console.error('No se completó la autorización de Drive. No se imprimieron claves ni tokens.');
    clearTimeout(limite); servidor.close(); process.exitCode = 1;
  }
});
await new Promise(resolve => servidor.listen(0, '127.0.0.1', resolve));
retorno = 'http://127.0.0.1:' + servidor.address().port + '/oauth2/callback';
const url = new URL('https://accounts.google.com/o/oauth2/v2/auth');
url.search = new URLSearchParams({ client_id: configuracion.client_id, redirect_uri: retorno, response_type: 'code',
  scope: 'https://www.googleapis.com/auth/drive', access_type: 'offline', prompt: 'consent', state: estado,
  code_challenge: desafio, code_challenge_method: 'S256' }).toString();
console.log('La cuenta que autorices se usará para el almacenamiento del sistema, no para iniciar sesión de los clientes.');
console.log('Google pedirá permiso de Drive porque las carpetas ya existen. El backend limita sus operaciones a las carpetas configuradas. Revisa el consentimiento antes de aceptar.');
console.log('Abre este enlace en tu navegador y autoriza con la cuenta propietaria de las carpetas:\n' + url.href);
const limite = setTimeout(() => { servidor.close(); console.error('El tiempo para autorizar terminó. Ejecuta el comando nuevamente.'); process.exitCode = 1; }, 300000);
