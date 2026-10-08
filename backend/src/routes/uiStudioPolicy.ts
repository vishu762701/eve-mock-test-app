/** Executable-action and hierarchy policy. Client protection flags cannot grant permissions. */
export function validateStudioPolicy(config: any): string[] {
  const errors: string[] = [];
  const record = (v: any) => v && typeof v === 'object' && !Array.isArray(v);
  if (!record(config) || !record(config.screens)) return ['screens must be an object'];
  for (const name of ['branding','designSystem']) {
    if(config[name] !== undefined && !record(config[name])) errors.push(`${name}: must be an object`);
    if(config[name]?.enabled !== undefined && typeof config[name].enabled !== 'boolean')errors.push(`${name}.enabled: must be boolean`);
  }
  const destinations = new Set(['home','profile','notifications','syllabus','settings','history','bookmarks','about']);
  const types = new Set(['text','button','image','icon','card','banner','divider','spacer']);
  const protectedLegacy = new Set(['timer_pill','question_card','question_text','bottom_actions','action_grid','btn_login','login_google','home_bottom_nav','result_bottom_bar']);
  const safeUrl = (value: any) => {
    try { const u = new URL(value); return typeof value === 'string' && ['http:','https:'].includes(u.protocol) && !!u.hostname && !u.username && !u.password && !value.includes('\\'); } catch { return false; }
  };
  for (const [screenKey,s] of Object.entries<any>(config.screens)) {
    if (!record(s) || (s.components !== undefined && !record(s.components))) { errors.push(`${screenKey}: components must be an object`); continue; }
    const comps = s.components || {};
    for (const [key,c] of Object.entries<any>(comps)) {
      if (!record(c)) { errors.push(`${key}: component must be an object`); continue; }
      for (const flag of ['visible','enabled','isProtected']) if(c[flag] !== undefined && typeof c[flag] !== 'boolean')errors.push(`${key}.${flag}: must be boolean`);
      if(c.parentId != null && typeof c.parentId !== 'string')errors.push(`${key}: parent must be an ID`);
      if(c.order !== undefined && (!Number.isInteger(c.order) || Math.abs(c.order)>100000))errors.push(`${key}: invalid order`);
      const custom = key.startsWith('custom_');
      if (c.id && c.id !== key) errors.push(`${key}: ID must match key`);
      if (custom && !types.has(c.type)) errors.push(`${key}: unsupported inserted type`);
      for (const category of ['layout','appearance','material','typography','content','actions','animation','states']) {
        if (c[category] !== undefined && !record(c[category])) errors.push(`${key}.${category}: must be an object`);
      }
      const a = c.actions || {};
      if (a.actionType && !['none','navigate','open_url'].includes(a.actionType)) errors.push(`${key}: action not allowed`);
      if (a.actionType === 'navigate' && !destinations.has(a.actionTarget)) errors.push(`${key}: destination not allowed`);
      if (a.actionType === 'open_url' && !safeUrl(a.actionTarget)) errors.push(`${key}: invalid external URL`);
      if (!custom && ((a.actionType && a.actionType !== 'none') || c.enabled === false)) errors.push(`${key}: native actions are protected`);
      if ((key.startsWith('native_') || c.isProtected || protectedLegacy.has(key)) && c.visible === false) errors.push(`${key}: essential element cannot be hidden`);
      if (custom && typeof c.parentId === "string" && c.parentId) {
        const p = comps[c.parentId];
        if (!p || !c.parentId.startsWith('custom_') || !['card','banner'].includes(p.type)) errors.push(`${key}: invalid inserted parent`);
      }
      const seen = new Set([key]); let parent = typeof c.parentId === "string" ? c.parentId : null;
      while (parent) {
        if (seen.has(parent)) { errors.push(`${key}: hierarchy cycle`); break; }
        seen.add(parent); const next = comps[parent]?.parentId;parent = typeof next === "string" ? next : null;
      }
      for(const value of [c.layout?.width,c.layout?.height]) {
        if(value != null) {
          const d=typeof value === 'string' ? value.trim().toLowerCase() : '';
          const numeric=/^[0-9]+(?:dp|px)?$/.test(d) && Number(d.replace(/(?:dp|px)$/,''))<=2000;
          if(typeof value !== 'string' || (!['match_parent','match','wrap_content','wrap','auto'].includes(d) && !numeric))errors.push(`${key}: dimensions must be 0–2000 dp or match/wrap`);
        }
      }
      if (c.content?.imageSource && !safeUrl(c.content.imageSource)) errors.push(`${key}: invalid image URL`);
      const color=/^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/;
      for(const value of [c.appearance?.iconTint,c.appearance?.highlightColor,c.states?.focusedBackgroundColor,c.states?.pressedBackgroundColor,c.states?.selectedBackgroundColor,c.states?.disabledBackgroundColor,c.states?.selectedTextColor]) {
        if(value!=null && (typeof value!=='string' || !color.test(value)))errors.push(`${key}: invalid visual color`);
      }
      if(c.appearance?.shape!=null && !['rounded','capsule','circle'].includes(c.appearance.shape))errors.push(`${key}: unsupported shape`);
      const highlight=c.appearance?.highlightOpacity, press=c.animation?.pressScale;
      if(highlight!=null && (typeof highlight!=='number' || !Number.isFinite(highlight) || highlight<0 || highlight>1))errors.push(`${key}: invalid highlight opacity`);
      if(press!=null && (typeof press!=='number' || !Number.isFinite(press) || press<.85 || press>1))errors.push(`${key}: invalid press scale`);
      if(c.animation?.stateTransitionMs!=null && (!Number.isInteger(c.animation.stateTransitionMs) || c.animation.stateTransitionMs<0 || c.animation.stateTransitionMs>600))errors.push(`${key}: state transition must be 0–600 ms`);
      for(const flag of ['springRelease','hapticFeedback']) if(c.animation?.[flag]!=null && typeof c.animation[flag]!=='boolean')errors.push(`${key}: invalid interaction flag`);
      if (c.material?.blurRadius != null && (!Number.isInteger(c.material.blurRadius) || c.material.blurRadius < 0 || c.material.blurRadius > 25)) errors.push(`${key}: blur must be 0–25`);
      if(c.typography?.textStyle!=null && !['normal','bold','italic','bold_italic'].includes(c.typography.textStyle))errors.push(`${key}: unsupported text style`);
      if(c.typography?.textAlign!=null && !['start','left','center','end','right'].includes(c.typography.textAlign))errors.push(`${key}: unsupported text alignment`);
      if(c.animation?.interpolator!=null && !['standard','ease_in','ease_out','spring','accelerate','decelerate','overshoot'].includes(c.animation.interpolator))errors.push(`${key}: unsupported response curve`);
      if (c.typography?.fontFamily && !['sans-serif','serif','monospace'].includes(c.typography.fontFamily)) errors.push(`${key}: unsupported font`);
      if (c.animation?.type && !['fade','scale','fade_scale','slide','pop'].includes(c.animation.type)) errors.push(`${key}: unsupported animation`);
      for (const value of [c.material?.materialOpacity,c.material?.tintOpacity,c.appearance?.opacity]) {
        if (value != null && (typeof value !== 'number' || !Number.isFinite(value) || value<0 || value>1)) errors.push(`${key}: opacity must be 0–1`);
      }
    }
  }
  return errors;
}
