"use client";

import { ScanLine } from "lucide-react";

export function QrScannerMock() {
  return (
    <div className="relative mx-auto h-64 w-64 overflow-hidden rounded-3xl bg-gradient-to-br from-brand-900 to-brand-700 shadow-xl">
      {[
        "top-3 left-3 border-t-2 border-l-2 rounded-tl-lg",
        "top-3 right-3 border-t-2 border-r-2 rounded-tr-lg",
        "bottom-3 left-3 border-b-2 border-l-2 rounded-bl-lg",
        "bottom-3 right-3 border-b-2 border-r-2 rounded-br-lg"
      ].map((c, i) => (
        <span key={i} className={`absolute h-8 w-8 border-brand-300/90 ${c}`} />
      ))}
      <div className="absolute inset-x-6 top-6 h-0.5 rounded-full laser-line animate-laser-scan" />
      <div className="absolute inset-0 flex items-center justify-center text-brand-200/70">
        <ScanLine className="h-12 w-12 opacity-60" />
      </div>
    </div>
  );
}
