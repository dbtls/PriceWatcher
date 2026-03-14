import type { PriceHistoryItemRes } from "@/lib/types";

type ChartPoint = {
  date: string;
  value: number;
};

type PriceTrendSeries = {
  id: string;
  name: string;
  color: string;
  points: PriceHistoryItemRes[];
};

interface PriceTrendChartProps {
  series: PriceTrendSeries[];
  height?: number;
}

const CHART_COLORS = [
  "#2563eb",
  "#f97316",
  "#16a34a",
  "#dc2626",
  "#7c3aed",
  "#0891b2",
];

const CHART_MARGIN = {
  top: 16,
  right: 16,
  bottom: 28,
  left: 76,
};

function normalizePoints(points: PriceHistoryItemRes[]): ChartPoint[] {
  return [...points]
    .map((point) => ({
      date: point.capturedAt,
      value:
        typeof point.price === "number" ? point.price : Number(point.price),
    }))
    .filter((point) => Number.isFinite(point.value))
    .sort(
      (a, b) =>
        new Date(a.date).getTime() - new Date(b.date).getTime()
    );
}

function formatPrice(value: number) {
  return `${Math.round(value).toLocaleString()}원`;
}

function formatDateLabel(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.slice(5, 10);
  }

  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function buildPath(
  points: ChartPoint[],
  width: number,
  height: number,
  minValue: number,
  maxValue: number
) {
  if (points.length === 0) {
    return "";
  }

  const innerWidth = width - CHART_MARGIN.left - CHART_MARGIN.right;
  const innerHeight = height - CHART_MARGIN.top - CHART_MARGIN.bottom;
  const xStep = points.length > 1 ? innerWidth / (points.length - 1) : 0;
  const valueRange = Math.max(maxValue - minValue, 1);

  return points
    .map((point, index) => {
      const x = CHART_MARGIN.left + xStep * index;
      const y =
        CHART_MARGIN.top +
        innerHeight -
        ((point.value - minValue) / valueRange) * innerHeight;
      return `${index === 0 ? "M" : "L"} ${x} ${y}`;
    })
    .join(" ");
}

export function PriceTrendChart({
  series,
  height = 320,
}: PriceTrendChartProps) {
  const preparedSeries = series
    .map((item, index) => ({
      ...item,
      color: item.color || CHART_COLORS[index % CHART_COLORS.length],
      normalizedPoints: normalizePoints(item.points),
    }))
    .filter((item) => item.normalizedPoints.length > 0);

  if (preparedSeries.length === 0) {
    return (
      <div className="rounded-2xl border border-border bg-white dark:bg-[var(--surface)] p-6 text-sm text-text-muted">
        표시할 가격 이력이 없습니다.
      </div>
    );
  }

  const allPoints = preparedSeries.flatMap((item) => item.normalizedPoints);
  const values = allPoints.map((point) => point.value);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const chartWidth = 840;
  const ticks = 4;
  const yTicks = Array.from({ length: ticks + 1 }, (_, index) => {
    const ratio = index / ticks;
    return maxValue - (maxValue - minValue) * ratio;
  });

  const xAxisPoints =
    preparedSeries.reduce<ChartPoint[]>(
      (longest, current) =>
        current.normalizedPoints.length > longest.length
          ? current.normalizedPoints
          : longest,
      preparedSeries[0].normalizedPoints
    ) ?? [];

  return (
    <div className="rounded-2xl border border-border bg-white dark:bg-[var(--surface)] p-4 sm:p-6">
      <div className="mb-4 flex flex-wrap gap-3">
        {preparedSeries.map((item) => (
          <div key={item.id} className="flex items-center gap-2 text-sm text-text-main">
            <span
              className="h-2.5 w-2.5 rounded-full"
              style={{ backgroundColor: item.color }}
              aria-hidden="true"
            />
            <span className="truncate">{item.name}</span>
          </div>
        ))}
      </div>

      <div className="overflow-x-auto">
        <svg
          viewBox={`0 0 ${chartWidth} ${height}`}
          className="min-w-[760px] w-full"
          role="img"
          aria-label="가격 추이 차트"
        >
          {yTicks.map((tick) => {
            const innerHeight = height - CHART_MARGIN.top - CHART_MARGIN.bottom;
            const valueRange = Math.max(maxValue - minValue, 1);
            const y =
              CHART_MARGIN.top +
              ((maxValue - tick) / valueRange) * innerHeight;

            return (
              <g key={tick}>
                <line
                  x1={CHART_MARGIN.left}
                  y1={y}
                  x2={chartWidth - CHART_MARGIN.right}
                  y2={y}
                  stroke="currentColor"
                  strokeOpacity="0.12"
                />
                <text
                  x={CHART_MARGIN.left - 10}
                  y={y + 4}
                  textAnchor="end"
                  fontSize="12"
                  fill="currentColor"
                  opacity="0.7"
                >
                  {formatPrice(tick)}
                </text>
              </g>
            );
          })}

          {xAxisPoints.map((point, index) => {
            const innerWidth = chartWidth - CHART_MARGIN.left - CHART_MARGIN.right;
            const xStep = xAxisPoints.length > 1 ? innerWidth / (xAxisPoints.length - 1) : 0;
            const x = CHART_MARGIN.left + xStep * index;

            return (
              <text
                key={`${point.date}-${index}`}
                x={x}
                y={height - 8}
                textAnchor="middle"
                fontSize="12"
                fill="currentColor"
                opacity="0.7"
              >
                {formatDateLabel(point.date)}
              </text>
            );
          })}

          {preparedSeries.map((item) => (
            <g key={item.id}>
              <path
                d={buildPath(
                  item.normalizedPoints,
                  chartWidth,
                  height,
                  minValue,
                  maxValue
                )}
                fill="none"
                stroke={item.color}
                strokeWidth="3"
                strokeLinejoin="round"
                strokeLinecap="round"
              />
              {item.normalizedPoints.map((point, index) => {
                const innerWidth = chartWidth - CHART_MARGIN.left - CHART_MARGIN.right;
                const innerHeight = height - CHART_MARGIN.top - CHART_MARGIN.bottom;
                const xStep =
                  item.normalizedPoints.length > 1
                    ? innerWidth / (item.normalizedPoints.length - 1)
                    : 0;
                const valueRange = Math.max(maxValue - minValue, 1);
                const x = CHART_MARGIN.left + xStep * index;
                const y =
                  CHART_MARGIN.top +
                  innerHeight -
                  ((point.value - minValue) / valueRange) * innerHeight;

                return (
                  <g key={`${item.id}-${point.date}-${index}`}>
                    <circle cx={x} cy={y} r="4" fill={item.color} />
                    <title>
                      {`${item.name} ${formatDateLabel(point.date)} ${formatPrice(
                        point.value
                      )}`}
                    </title>
                  </g>
                );
              })}
            </g>
          ))}
        </svg>
      </div>
    </div>
  );
}
