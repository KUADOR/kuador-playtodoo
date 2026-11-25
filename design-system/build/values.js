/**
 * Serializes resolved DTCG token values for each target language.
 * One serializer per output language (Strategy): formats only decide layout, not values.
 */

const PX_PATTERN = /^(-?\d+(?:\.\d+)?)px$/;
const HEX_PATTERN = /^#([0-9a-f]{6})$/i;
const RGBA_PATTERN = /^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d*\.?\d+)\s*\)$/i;
const GENERIC_FONT_FAMILIES = new Set(['serif', 'sans-serif', 'monospace', 'system-ui', 'cursive', 'fantasy']);

export const tokenValue = (token) => token.$value ?? token.value;
const tokenType = (token) => token.$type ?? token.type;

function pxToNumber(dimension) {
  const match = PX_PATTERN.exec(dimension);
  if (!match) {
    throw new Error(`Dimension "${dimension}" must be expressed in px`);
  }
  return Number(match[1]);
}

function cssFontFamily(families) {
  return families.map((family) => (GENERIC_FONT_FAMILIES.has(family) ? family : `"${family}"`)).join(', ');
}

function cssShadow({ offsetX, offsetY, blur, spread, color }) {
  return `${offsetX} ${offsetY} ${blur} ${spread} ${color}`;
}

/** CSS and TypeScript share the same textual representation. */
export function toCss(token) {
  const value = tokenValue(token);
  switch (tokenType(token)) {
    case 'fontFamily':
      return cssFontFamily(value);
    case 'shadow':
      return cssShadow(value);
    default:
      return String(value);
  }
}

export function toTs(token) {
  const type = tokenType(token);
  if (type === 'fontWeight' || type === 'number') {
    return String(tokenValue(token));
  }
  return `'${toCss(token)}'`;
}

function dartDouble(number) {
  return Number.isInteger(number) ? `${number}.0` : String(number);
}

function dartColor(color) {
  const hex = HEX_PATTERN.exec(color);
  if (hex) {
    return `Color(0xFF${hex[1].toUpperCase()})`;
  }
  const rgba = RGBA_PATTERN.exec(color);
  if (rgba) {
    const [, red, green, blue, alpha] = rgba;
    return `Color.fromRGBO(${red}, ${green}, ${blue}, ${Number(alpha)})`;
  }
  throw new Error(`Unsupported color "${color}": use #RRGGBB or rgba()`);
}

function dartShadow({ offsetX, offsetY, blur, spread, color }) {
  const offset = `Offset(${dartDouble(pxToNumber(offsetX))}, ${dartDouble(pxToNumber(offsetY))})`;
  return (
    `[BoxShadow(color: ${dartColor(color)}, offset: ${offset}, ` +
    `blurRadius: ${dartDouble(pxToNumber(blur))}, spreadRadius: ${dartDouble(pxToNumber(spread))})]`
  );
}

const DART_SERIALIZERS = {
  color: { type: 'Color', value: dartColor },
  dimension: { type: 'double', value: (value) => dartDouble(pxToNumber(value)) },
  number: { type: 'double', value: dartDouble },
  fontWeight: { type: 'FontWeight', value: (value) => `FontWeight.w${value}` },
  fontFamily: { type: 'String', value: (families) => `'${families[0]}'` },
  shadow: { type: 'List<BoxShadow>', value: dartShadow },
};

export function toDart(token) {
  const serializer = DART_SERIALIZERS[tokenType(token)];
  if (!serializer) {
    throw new Error(`Token "${token.path.join('.')}" has unsupported type "${tokenType(token)}" for Dart`);
  }
  return { type: serializer.type, value: serializer.value(tokenValue(token)) };
}
