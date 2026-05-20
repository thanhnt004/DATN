import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import {
  Loader2, TrendingUp, ShoppingBag, DollarSign,
  ArrowUpRight, ArrowDownRight, BarChart3, PieChart as PieChartIcon, Percent, PackageX,
  Award, PackageSearch
} from "lucide-react";
import {
  BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid,
  Tooltip as RechartsTooltip, ResponsiveContainer,
  LineChart, Line, PieChart, Pie, Legend
} from "recharts";
import { getSellerDetailedStats, getSellerDetailedProducts } from "../../api/sellerDashboardApi";
import type { DetailedReportResponse, ProductSalesData } from "../../types/order";
import type { PageResponse } from "../../types/user";

// ─── helpers ───────────────────────────────────────────────────────────────────

function fmt(n: number | null | undefined) {
  return (n ?? 0).toLocaleString("vi-VN");
}

function fmtCurrency(n: number | null | undefined) {
  return (n ?? 0).toLocaleString("vi-VN") + "₫";
}

function StatCard({
  icon: Icon, label, value, sub, color, trend,
}: {
  icon: React.ElementType; label: string; value: string; sub?: string;
  color: string; trend?: "up" | "down" | null;
}) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-start gap-3 shadow-sm">
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${color}`}>
        <Icon className="w-5 h-5" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-slate-400 font-semibold uppercase tracking-widest truncate">{label}</p>
        <div className="flex items-center gap-2 mt-1">
          <p className="text-lg sm:text-xl font-black text-slate-800 truncate">{value}</p>
          {trend === "up" && <ArrowUpRight className="w-4 h-4 text-green-500 shrink-0" />}
          {trend === "down" && <ArrowDownRight className="w-4 h-4 text-red-500 shrink-0" />}
        </div>
        {sub && <p className="text-[11px] text-slate-400 mt-0.5 truncate">{sub}</p>}
      </div>
    </div>
  );
}

// ─── Chart Tooltips ──────────────────────────────────────────────────────────

const CurrencyTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.[0]) return null;
  return (
    <div className="bg-white border border-slate-200 rounded-lg px-3 py-2 shadow-lg">
      <p className="text-xs font-semibold text-slate-800 mb-1">{label}</p>
      {payload.map((entry: any, index: number) => {
        const isRange = Array.isArray(entry.value);
        let val = isRange ? entry.value[1] - entry.value[0] : entry.value;
        return (
          <p key={index} className="text-sm font-bold" style={{ color: entry.color || entry.fill }}>
            {entry.name}: {fmtCurrency(Math.abs(val))}
          </p>
        );
      })}
    </div>
  );
};

const TrendTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.[0]) return null;
  return (
    <div className="bg-white border border-slate-200 rounded-lg px-3 py-2 shadow-lg min-w-[160px]">
      <p className="text-xs font-semibold text-slate-800 mb-2 border-b pb-1">{label}</p>
      {payload.map((entry: any, index: number) => (
        <div key={index} className="flex justify-between items-center gap-4 mb-1">
          <span className="text-xs font-medium text-slate-600">{entry.name}:</span>
          <span className="text-sm font-bold" style={{ color: entry.color }}>
            {fmtCurrency(entry.value)}
          </span>
        </div>
      ))}
    </div>
  );
};

const CustomPieTooltip = ({ active, payload }: any) => {
  if (!active || !payload?.[0]) return null;
  const data = payload[0].payload;
  return (
    <div className="bg-white border border-slate-200 rounded-lg px-3 py-2 shadow-lg">
      <p className="text-xs font-semibold text-slate-800 mb-1">{data.categoryName}</p>
      <p className="text-sm font-bold text-indigo-600">
        Doanh thu: {fmtCurrency(data.revenue)}
      </p>
      <p className="text-xs text-slate-500 font-medium mt-1">
        Chiếm: {data.percentage?.toFixed(1) || 0}%
      </p>
    </div>
  );
};

// ─── Main page ─────────────────────────────────────────────────────────────────

export default function SellerRevenuePage() {
  const [stats, setStats] = useState<DetailedReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [productLoading, setProductLoading] = useState(true);
  const [period, setPeriod] = useState<"DAILY" | "WEEKLY" | "MONTHLY">("DAILY");
  const [dateRange, setDateRange] = useState<"7d" | "30d" | "90d" | "all">("30d");
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState<"revenue" | "quantity">("revenue");
  const [sortDir, setSortDir] = useState<"DESC" | "ASC">("DESC");
  const size = 10;
  const [productsPage, setProductsPage] = useState<PageResponse<ProductSalesData> | null>(null);

  const rangeParams = useMemo(() => {
    let startDate: string | undefined = undefined;
    let endDate: string | undefined = undefined;
    if (dateRange !== "all") {
      const end = new Date();
      const start = new Date();
      if (dateRange === "7d") start.setDate(end.getDate() - 7);
      else if (dateRange === "30d") start.setDate(end.getDate() - 30);
      else if (dateRange === "90d") start.setDate(end.getDate() - 90);

      startDate = start.toISOString().split('T')[0];
      endDate = end.toISOString().split('T')[0];
    }
    return { startDate, endDate };
  }, [dateRange]);

  useEffect(() => {
    setPage(0);
  }, [rangeParams, period, sortBy, sortDir]);

  useEffect(() => {
    let isMounted = true;
    const fetchStats = async () => {
      setLoading(true);
      try {
        const data = await getSellerDetailedStats({
          startDate: rangeParams.startDate,
          endDate: rangeParams.endDate,
          period,
        });
        if (isMounted) setStats(data);
      } catch (error) {
        console.error("Failed to fetch detailed stats", error);
      } finally {
        if (isMounted) setLoading(false);
      }
    };
    fetchStats();
    return () => { isMounted = false; };
  }, [period, rangeParams]);

  const trendData = useMemo(() => {
    if (!stats) return [];
    const current = stats.revenueByTime || [];
    const previous = stats.previousRevenueByTime || [];
    return current.map((c, i) => ({
      period: c.period,
      revenue: c.revenue,
      orderCount: c.orderCount,
      previousRevenue: previous[i]?.revenue || 0,
    }));
  }, [stats]);

  useEffect(() => {
    let isMounted = true;
    const fetchProductPages = async () => {
      setProductLoading(true);
      try {
        const data = await getSellerDetailedProducts({
          startDate: rangeParams.startDate,
          endDate: rangeParams.endDate,
          period,
          page,
          size,
          sortBy,
          sortDir,
        });
        if (isMounted) {
          setProductsPage(data.productsPage ?? null);
        }
      } catch (error) {
        console.error("Failed to fetch product pages", error);
      } finally {
        if (isMounted) setProductLoading(false);
      }
    };
    fetchProductPages();
    return () => { isMounted = false; };
  }, [period, rangeParams, page, sortBy, sortDir]);

  if (loading && !stats) {
    return (
      <div className="flex items-center justify-center py-24 gap-2 text-slate-400">
        <Loader2 className="w-5 h-5 animate-spin" /><span className="text-sm font-semibold">Đang tải báo cáo...</span>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="flex items-center justify-center py-24 text-slate-500">
        Không thể tải dữ liệu báo cáo. Vui lòng thử lại sau.
      </div>
    );
  }

  const formatYAxisCurrency = (value: number) => {
    if (value >= 1000000000) return `${(value / 1000000000).toFixed(1)}B`;
    if (value >= 1000000) return `${(value / 1000000).toFixed(1)}M`;
    if (value >= 1000) return `${(value / 1000).toFixed(0)}K`;
    return `${value}`;
  };

  const waterfallChart = stats.waterfallChart || {
    baseProductRevenue: 0, shopVouchers: 0, shippingSubsidies: 0, platformFees: 0,
    paymentFees: 0, commissionFees: 0, serviceFees: 0, payout: 0,
  };

  const products = productsPage?.content ?? [];
  const pageNumber = productsPage?.number ?? 0;
  let current = waterfallChart.baseProductRevenue || 0;
  
  const v = waterfallChart.shopVouchers || 0;
  const rangeV: [number, number] = [current - v, current]; current -= v;
  const s = waterfallChart.shippingSubsidies || 0;
  const rangeS: [number, number] = [current - s, current]; current -= s;
  const pl = waterfallChart.platformFees || 0;
  const rangePl: [number, number] = [current - pl, current]; current -= pl;
  const pf = waterfallChart.paymentFees || 0;
  const rangePf: [number, number] = [current - pf, current]; current -= pf;
  const cf = waterfallChart.commissionFees || 0;
  const rangeCf: [number, number] = [current - cf, current]; current -= cf;
  const sf = waterfallChart.serviceFees || 0;
  const rangeSf: [number, number] = [current - sf, current]; current -= sf;

  const waterfallData = [
    { name: "Doanh thu gốc", range: [0, waterfallChart.baseProductRevenue || 0], fill: "#4ade80" },
    { name: "Voucher Shop", range: rangeV, fill: "#f87171" },
    { name: "Phí vận chuyển (Shop)", range: rangeS, fill: "#f87171" },
    { name: "Tổng phí sàn", range: rangePl, fill: "#facc15" },
    { name: "Phí thanh toán", range: rangePf, fill: "#facc15" },
    { name: "Phí cố định/Hoa hồng", range: rangeCf, fill: "#facc15" },
    { name: "Phí dịch vụ", range: rangeSf, fill: "#facc15" },
    { name: "Thực nhận (Payout)", range: [0, waterfallChart.payout || 0], fill: "#3b82f6" },
  ];

  const COLORS = ['#6366f1', '#ec4899', '#14b8a6', '#f59e0b', '#8b5cf6'];

  return (
    <div className="space-y-6 max-w-6xl pb-10">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900">Báo cáo doanh thu</h1>
          <p className="text-sm text-slate-500 mt-1">
            Theo dõi chi tiết doanh số, các khoản phí và thực nhận
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center bg-white border border-slate-200 rounded-lg p-1 shadow-sm">
            {(["7d", "30d", "90d", "all"] as const).map(p => (
              <button key={p} onClick={() => setDateRange(p)}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                  dateRange === p ? "bg-slate-100 text-slate-800" : "text-slate-500 hover:text-slate-700"
                }`}>
                {p === "all" ? "Tất cả" : p === "7d" ? "7 ngày" : p === "30d" ? "30 ngày" : "90 ngày"}
              </button>
            ))}
          </div>
          <div className="flex items-center bg-white border border-slate-200 rounded-lg p-1 shadow-sm">
            {(["DAILY", "WEEKLY", "MONTHLY"] as const).map(p => (
              <button key={p} onClick={() => setPeriod(p)}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                  period === p ? "bg-blue-50 text-blue-700" : "text-slate-500 hover:text-slate-700"
                }`}>
                {p === "DAILY" ? "Ngày" : p === "WEEKLY" ? "Tuần" : "Tháng"}
              </button>
            ))}
          </div>
        </div>
      </div>

      {loading && (
        <div className="h-1 bg-blue-100 overflow-hidden rounded-full">
          <div className="h-full bg-blue-500 w-1/3 animate-[slide_1s_ease-in-out_infinite]"></div>
        </div>
      )}

      {/* 1. Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-3 gap-4">
        <StatCard icon={DollarSign} label="Tổng doanh số" value={fmtCurrency(stats.totalGrossSales)} color="bg-green-100 text-green-600" />
        <StatCard icon={TrendingUp} label="Doanh thu thuần" value={fmtCurrency(stats.netRevenue)} color="bg-blue-100 text-blue-600" />
        <StatCard icon={ShoppingBag} label="Tổng số đơn" value={fmt(stats.totalOrderCount)} color="bg-purple-100 text-purple-600" />
        <StatCard icon={Percent} label="Giá trị TB/đơn (AOV)" value={fmtCurrency(stats.averageOrderValue)} color="bg-emerald-100 text-emerald-600" />
        <StatCard icon={PackageX} label="Tỷ lệ hủy" value={`${(stats.cancellationRate || 0).toFixed(1)}%`} color="bg-red-100 text-red-600" trend={(stats.cancellationRate || 0) > 5 ? "up" : "down"} />
        <StatCard icon={ArrowDownRight} label="Tỷ lệ hoàn" value={`${(stats.returnRate || 0).toFixed(1)}%`} color="bg-orange-100 text-orange-600" trend={(stats.returnRate || 0) > 5 ? "up" : "down"} />
      </div>

      {/* 2. Trend & Order Count Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Trend Line Chart */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-indigo-600" />
              <h3 className="font-bold text-slate-800 text-base">Xu hướng Doanh thu</h3>
            </div>
          </div>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trendData} margin={{ top: 10, right: 10, left: 0, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="period" stroke="#64748b" style={{ fontSize: "11px" }} tickMargin={10} />
                <YAxis stroke="#64748b" style={{ fontSize: "11px" }} tickFormatter={formatYAxisCurrency} />
                <RechartsTooltip content={<TrendTooltip />} cursor={{ stroke: "#cbd5e1", strokeWidth: 1, strokeDasharray: "3 3" }} />
                <Legend verticalAlign="top" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px', fontWeight: 500 }} />
                <Line type="monotone" dataKey="revenue" name="Kỳ hiện tại" stroke="#6366f1" strokeWidth={3} dot={{ r: 4, strokeWidth: 2 }} activeDot={{ r: 6 }} />
                <Line type="monotone" dataKey="previousRevenue" name="Cùng kỳ trước" stroke="#cbd5e1" strokeWidth={2} strokeDasharray="5 5" dot={false} activeDot={{ r: 4 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Order Count Bar Chart */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-6">
            <ShoppingBag className="w-5 h-5 text-emerald-600" />
            <div>
              <h3 className="font-bold text-slate-800 text-base">Số lượng đơn hàng</h3>
              <p className="text-xs text-slate-500">Phát hiện các ngày/khung giờ cao điểm</p>
            </div>
          </div>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={trendData} margin={{ top: 10, right: 10, left: 0, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="period" stroke="#64748b" style={{ fontSize: "11px" }} tickMargin={10} />
                <YAxis stroke="#64748b" style={{ fontSize: "11px" }} allowDecimals={false} />
                <RechartsTooltip cursor={{ fill: "rgba(241, 245, 249, 0.5)" }} contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '13px', fontWeight: 600 }} />
                <Bar dataKey="orderCount" name="Số đơn hàng" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={40} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* 3. Categories & Waterfall */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Categories Pie Chart */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-6">
            <PieChartIcon className="w-5 h-5 text-fuchsia-600" />
            <h3 className="font-bold text-slate-800 text-base">Tỷ trọng doanh thu theo Thể loại</h3>
          </div>
          {stats.revenueByCategory && stats.revenueByCategory.length > 0 ? (
            <div className="h-[300px] flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={stats.revenueByCategory}
                    cx="50%" cy="45%"
                    innerRadius={60} outerRadius={100}
                    paddingAngle={2}
                    dataKey="revenue"
                    nameKey="categoryName"
                    label={({ cx, cy, midAngle, innerRadius, outerRadius, index }) => {
                      if (midAngle === undefined || index === undefined) return null;
                      const RADIAN = Math.PI / 180;
                      const radius = 25 + innerRadius + (outerRadius - innerRadius);
                      const x = cx + radius * Math.cos(-midAngle * RADIAN);
                      const y = cy + radius * Math.sin(-midAngle * RADIAN);
                      return (
                        <text x={x} y={y} fill="#475569" textAnchor={x > cx ? 'start' : 'end'} dominantBaseline="central" fontSize={11} fontWeight={600}>
                          {stats.revenueByCategory[index]?.categoryName}
                        </text>
                      );
                    }}
                  >
                    {stats.revenueByCategory.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip content={<CustomPieTooltip />} />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" wrapperStyle={{ fontSize: '12px', fontWeight: 500 }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-[300px] flex items-center justify-center text-sm text-slate-500">
              Không có dữ liệu thể loại
            </div>
          )}
        </div>

        {/* Waterfall Chart */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-center gap-2 mb-6">
            <BarChart3 className="w-5 h-5 text-blue-600" />
            <div>
              <h3 className="font-bold text-slate-800 text-base">Phân tích dòng tiền </h3>
            </div>
          </div>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={waterfallData} margin={{ top: 20, right: 30, left: 20, bottom: 40 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="name" stroke="#64748b" style={{ fontSize: "11px" }} angle={-30} textAnchor="end" height={60} />
                <YAxis stroke="#64748b" style={{ fontSize: "12px" }} tickFormatter={formatYAxisCurrency} />
                <RechartsTooltip content={<CurrencyTooltip />} cursor={{ fill: "rgba(241, 245, 249, 0.5)" }} />
                <Bar dataKey="range" radius={[4, 4, 4, 4]} isAnimationActive={false}>
                  {waterfallData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* 4. Product Sales */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mb-6">
          <div className="flex items-center gap-2">
            <Award className="w-5 h-5 text-amber-500" />
            <h3 className="font-bold text-slate-800 text-base">Thống kê sản phẩm</h3>
          </div>
          <div className="flex items-center gap-3">
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              className="px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-semibold outline-none focus:border-indigo-400 bg-white"
            >
              <option value="revenue">Theo doanh thu</option>
              <option value="quantity">Theo số lượng</option>
            </select>
            <select
              value={sortDir}
              onChange={(e) => setSortDir(e.target.value as any)}
              className="px-3 py-1.5 rounded-lg border border-slate-200 text-sm font-semibold outline-none focus:border-indigo-400 bg-white"
            >
              <option value="DESC">Cao xuống thấp</option>
              <option value="ASC">Thấp đến cao</option>
            </select>
          </div>
        </div>
        {productLoading ? (
          <div className="flex items-center justify-center py-10 text-sm text-slate-500">
            <Loader2 className="w-5 h-5 animate-spin mr-2" /> Đang tải sản phẩm...
          </div>
        ) : products.length > 0 ? (
          <div className="space-y-3">
            {products.map((product, index) => (
              <Link key={product.productId} to={`/product/${product.productId}`}
                className="group flex items-center gap-4 rounded-2xl p-3 hover:bg-slate-50 transition-colors border border-transparent hover:border-slate-100">
                <div className="w-6 h-6 flex items-center justify-center bg-amber-100 text-amber-700 font-bold rounded-full text-xs shrink-0">
                  {index + 1 + pageNumber * size}
                </div>
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.productName} className="w-12 h-12 rounded object-cover border" />
                ) : (
                  <div className="w-12 h-12 rounded bg-slate-100 flex items-center justify-center border shrink-0">
                    <PackageSearch className="w-5 h-5 text-slate-400" />
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-slate-800 truncate group-hover:text-red-600">{product.productName}</p>
                  <p className="text-xs text-slate-500">Đã bán: <span className="font-medium text-slate-700">{fmt(product.totalQuantitySold)}</span></p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-indigo-600">{fmtCurrency(product.totalRevenue)}</p>
                </div>
              </Link>
            ))}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-slate-100 mt-2">
              <span className="text-xs text-slate-500">
                Trang {pageNumber + 1} / {productsPage?.totalPages || 1}
              </span>
              <div className="flex items-center gap-2">
                <button type="button" onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={productsPage?.first}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-100 disabled:opacity-50 disabled:cursor-not-allowed">
                  Trước
                </button>
                <button type="button" onClick={() => setPage(p => p + 1)}
                  disabled={productsPage?.last}
                  className="px-3 py-1.5 rounded-xl bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed">
                  Tiếp
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center py-10 text-sm text-slate-500">Chưa có dữ liệu sản phẩm</div>
        )}
      </div>
    </div>
  );
}
