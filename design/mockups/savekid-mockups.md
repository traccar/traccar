# SaveKid – propuesta de mockups con línea gráfica azul

A continuación se presentan mockups de referencia utilizando el logo proporcionado y una línea gráfica centrada en el azul "SaveKid" (#0D84FF). Los ejemplos muestran tanto vistas web como móviles para acompañar el flujo principal de localización y alertas de la plataforma.

## Identidad visual
- **Logo**: [`savekid-logo.svg`](../assets/savekid-logo.svg)
- **Color primario**: #0D84FF (acciones, iconografía y enlaces)
- **Secundario**: #0A5CCD (headers, sombras suaves)
- **Fondos claros**: #F4F8FF (paneles y tarjetas)
- **Estado**: éxito #2ED47A, alerta #FFB547, crítico #FF5C5C
- **Tipografía sugerida**: Inter / Work Sans, peso medio y semibold para títulos.

## Componentes base
- **App bar** en azul primario con el isotipo a la izquierda y botones de acción con esquinas redondeadas.
- **Tarjetas elevadas** con borde fino (#E4ECF7) y relleno claro para separar información de ubicación, batería y última actualización.
- **Botones**: primario sólido azul, secundario contorneado en azul, estados de hover con oscurecimiento leve.
- **Etiquetas de estado**: chips con fondo translúcido (por ejemplo, azul 12% de opacidad) y texto en el color de estado.

## Mockup 1 – Inicio web (escritorio)
<div style="border:1px solid #dce6f5; padding:18px; border-radius:12px; background:#f9fbff; font-family:Inter, 'Work Sans', system-ui; color:#0e1b2c;">
  <div style="display:flex; align-items:center; gap:12px; margin-bottom:16px;">
    <img src="../assets/savekid-logo.svg" alt="SaveKid" width="64" height="64" />
    <div>
      <div style="font-size:18px; font-weight:700; color:#0d84ff;">SaveKid</div>
      <div style="font-size:13px; color:#4b5563;">Panel de inicio · Ubicaciones recientes</div>
    </div>
    <div style="margin-left:auto; display:flex; gap:10px;">
      <button style="background:#0d84ff; color:white; border:none; padding:10px 16px; border-radius:10px; font-weight:600;">Agregar dispositivo</button>
      <button style="border:1px solid #0d84ff; color:#0d84ff; background:transparent; padding:10px 16px; border-radius:10px; font-weight:600;">Compartir ubicación</button>
    </div>
  </div>
  <div style="display:grid; grid-template-columns:2fr 1fr; gap:18px;">
    <div style="background:#fff; border:1px solid #e4ecf7; border-radius:14px; padding:16px; box-shadow:0 10px 30px rgba(13,132,255,0.08);">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
        <div style="font-weight:700; color:#0e1b2c;">Mapa en vivo</div>
        <span style="background:rgba(13,132,255,0.12); color:#0d84ff; padding:6px 10px; border-radius:10px; font-size:12px; font-weight:700;">EN LÍNEA</span>
      </div>
      <div style="height:260px; border-radius:12px; background:linear-gradient(135deg, #dceeff, #f4f8ff); display:flex; align-items:center; justify-content:center; color:#0a5ccd; font-weight:700;">Mapa con puntos activos</div>
    </div>
    <div style="display:grid; gap:12px;">
      <div style="background:#fff; border:1px solid #e4ecf7; border-radius:14px; padding:14px; box-shadow:0 10px 30px rgba(13,132,255,0.08);">
        <div style="font-weight:700; color:#0e1b2c; margin-bottom:6px;">Dispositivo principal</div>
        <div style="display:flex; justify-content:space-between; font-size:13px; color:#4b5563;">
          <span>Última actualización</span><span>Hace 2 min</span>
        </div>
        <div style="display:flex; justify-content:space-between; font-size:13px; color:#4b5563;">
          <span>Batería</span><span style="color:#2ed47a; font-weight:700;">82%</span>
        </div>
        <div style="margin-top:10px; display:flex; gap:8px;">
          <button style="background:#0d84ff; color:white; border:none; padding:8px 12px; border-radius:10px; font-weight:600;">Ver ruta</button>
          <button style="border:1px solid #e4ecf7; color:#0d84ff; background:#f4f8ff; padding:8px 12px; border-radius:10px; font-weight:600;">Alertas</button>
        </div>
      </div>
      <div style="background:#0d84ff; color:white; border-radius:14px; padding:14px; box-shadow:0 12px 32px rgba(13,132,255,0.24);">
        <div style="font-weight:700; margin-bottom:4px;">Zona segura activada</div>
        <div style="font-size:13px; opacity:0.92;">Notifica cuando se salga del perímetro definido.</div>
      </div>
    </div>
  </div>
</div>

## Mockup 2 – Vista móvil de seguimiento
<div style="display:flex; gap:16px; font-family:Inter, 'Work Sans', system-ui;">
  <div style="width:240px; border:1px solid #dce6f5; border-radius:24px; padding:12px; background:#f9fbff;">
    <div style="background:#0d84ff; color:white; border-radius:16px 16px 6px 6px; padding:12px; display:flex; align-items:center; gap:10px;">
      <img src="../assets/savekid-logo.svg" width="36" alt="SaveKid" />
      <div style="font-weight:700;">SaveKid</div>
      <span style="margin-left:auto; background:rgba(255,255,255,0.2); padding:4px 8px; border-radius:10px; font-size:11px;">EN LÍNEA</span>
    </div>
    <div style="margin-top:10px; background:#fff; border:1px solid #e4ecf7; border-radius:16px; padding:12px; box-shadow:0 10px 24px rgba(13,132,255,0.12);">
      <div style="font-weight:700; color:#0e1b2c;">Mapa</div>
      <div style="margin:8px 0 10px; height:140px; border-radius:12px; background:linear-gradient(135deg,#dceeff,#f4f8ff); display:flex; align-items:center; justify-content:center; color:#0a5ccd; font-weight:700;">Pin activo</div>
      <div style="display:flex; justify-content:space-between; font-size:12px; color:#4b5563;">
        <span>Última señal</span><span style="font-weight:700; color:#0d84ff;">Hace 2 min</span>
      </div>
      <div style="display:flex; gap:8px; margin-top:10px;">
        <button style="flex:1; background:#0d84ff; color:white; border:none; padding:8px; border-radius:10px; font-weight:700;">Ruta</button>
        <button style="flex:1; border:1px solid #0d84ff; color:#0d84ff; background:transparent; padding:8px; border-radius:10px; font-weight:700;">Zona segura</button>
      </div>
    </div>
  </div>
  <div style="width:240px; border:1px solid #dce6f5; border-radius:24px; padding:12px; background:#f9fbff;">
    <div style="background:#fff; border:1px solid #e4ecf7; border-radius:16px; padding:12px; box-shadow:0 10px 24px rgba(13,132,255,0.12); height:100%;">
      <div style="font-weight:700; color:#0e1b2c; margin-bottom:6px;">Alertas recientes</div>
      <div style="font-size:12px; color:#4b5563; display:flex; justify-content:space-between; align-items:center; padding:8px; border-radius:10px; background:#f4f8ff; margin-bottom:8px;">
        <span>Salida de zona segura</span>
        <span style="color:#ffb547; font-weight:700;">Hace 5 min</span>
      </div>
      <div style="font-size:12px; color:#4b5563; display:flex; justify-content:space-between; align-items:center; padding:8px; border-radius:10px; background:#f4f8ff; margin-bottom:8px;">
        <span>Batería baja</span>
        <span style="color:#ff5c5c; font-weight:700;">15%</span>
      </div>
      <button style="width:100%; background:#0d84ff; color:white; border:none; padding:9px; border-radius:10px; font-weight:700;">Ver historial</button>
    </div>
  </div>
</div>

## Mockup 3 – Timeline de alertas (web)
<div style="border:1px solid #dce6f5; padding:18px; border-radius:14px; background:#fff; font-family:Inter, 'Work Sans', system-ui; color:#0e1b2c; box-shadow:0 10px 30px rgba(13,132,255,0.08);">
  <div style="display:flex; align-items:center; gap:10px; margin-bottom:10px;">
    <img src="../assets/savekid-logo.svg" alt="SaveKid" width="44" />
    <div style="font-weight:800; font-size:18px; color:#0d84ff;">Alertas y seguridad</div>
  </div>
  <div style="display:grid; grid-template-columns:2fr 1fr; gap:14px;">
    <div>
      <div style="font-size:13px; color:#4b5563; margin-bottom:8px;">Resumen de eventos</div>
      <div style="border-left:3px solid #0d84ff; padding-left:12px; display:flex; flex-direction:column; gap:10px;">
        <div style="background:#f4f8ff; border:1px solid #e4ecf7; border-radius:12px; padding:10px;">
          <div style="display:flex; justify-content:space-between; font-weight:700;">Salida de zona segura<span style="color:#ffb547; font-size:12px;">08:42</span></div>
          <div style="font-size:12px; color:#4b5563;">Parque Central · Radio 300 m</div>
        </div>
        <div style="background:#f4f8ff; border:1px solid #e4ecf7; border-radius:12px; padding:10px;">
          <div style="display:flex; justify-content:space-between; font-weight:700;">Velocidad elevada<span style="color:#0d84ff; font-size:12px;">08:30</span></div>
          <div style="font-size:12px; color:#4b5563;">45 km/h cerca de casa</div>
        </div>
        <div style="background:#f4f8ff; border:1px solid #e4ecf7; border-radius:12px; padding:10px;">
          <div style="display:flex; justify-content:space-between; font-weight:700;">Batería baja<span style="color:#ff5c5c; font-size:12px;">08:05</span></div>
          <div style="font-size:12px; color:#4b5563;">15% restante · Recordar cargar</div>
        </div>
      </div>
    </div>
    <div style="background:#f9fbff; border:1px solid #e4ecf7; border-radius:12px; padding:12px;">
      <div style="font-weight:700; margin-bottom:6px;">Acciones rápidas</div>
      <button style="width:100%; background:#0d84ff; color:white; border:none; padding:10px; border-radius:10px; font-weight:700; margin-bottom:8px;">Activar zona segura</button>
      <button style="width:100%; border:1px solid #0d84ff; color:#0d84ff; background:transparent; padding:10px; border-radius:10px; font-weight:700;">Compartir en vivo</button>
      <div style="margin-top:12px; font-size:12px; color:#4b5563;">Consejo: conserva coherencia usando el azul SaveKid para acciones principales y tarjetas con fondos muy claros para jerarquía.</div>
    </div>
  </div>
</div>

---
Los mockups priorizan la visibilidad del logo, jerarquía clara en botones y tarjetas, y un uso consistente del azul SaveKid para acciones principales y señales de estado.
