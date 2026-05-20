import { useState, useEffect, lazy, Suspense } from "react";

const DesktopPage = lazy(() => import("./ProductDetailsPage"));
const MobilePage = lazy(() => import("./MobileProductDetailPage"));

const MOBILE_BREAKPOINT = 768;

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < MOBILE_BREAKPOINT);

  useEffect(() => {
    const mq = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`);
    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches);
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, []);

  return isMobile;
}

export default function ResponsiveProductPage() {
  const isMobile = useIsMobile();

  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-gray-100 flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-500" />
        </div>
      }
    >
      {isMobile ? <MobilePage /> : <DesktopPage />}
    </Suspense>
  );
}
