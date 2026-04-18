"use client";

import { Download } from "lucide-react";
import QRCode from "qrcode";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";

interface CipherQrCardProps {
  cipher: string;
  eventName: string;
}

/**
 * Renders the session cipher as a scannable QR code with PNG / SVG download buttons.
 * The on-screen canvas uses the Editorial Ivory ink/paper colours to sit in the page;
 * the downloaded PNG / SVG use pure black-on-white so they print cleanly on event badges.
 */
export function CipherQrCard({ cipher, eventName }: CipherQrCardProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [svg, setSvg] = useState<string>("");

  useEffect(() => {
    if (!cipher) return;
    if (canvasRef.current) {
      QRCode.toCanvas(canvasRef.current, cipher, {
        errorCorrectionLevel: "H",
        width: 256,
        margin: 2,
        color: { dark: "#16231f", light: "#faf6ee" }
      }).catch(() => null);
    }
    QRCode.toString(cipher, {
      type: "svg",
      errorCorrectionLevel: "H",
      margin: 2,
      color: { dark: "#000000", light: "#ffffff" }
    })
      .then(setSvg)
      .catch(() => setSvg(""));
  }, [cipher]);

  function triggerDownload(href: string, filename: string) {
    const a = document.createElement("a");
    a.href = href;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  const downloadPng = async () => {
    if (!cipher) return;
    // Re-render at print resolution (1024 px ≈ 8.7 cm at 300 dpi) on white background.
    const data = await QRCode.toDataURL(cipher, {
      errorCorrectionLevel: "H",
      width: 1024,
      margin: 2,
      color: { dark: "#000000", light: "#ffffff" }
    });
    triggerDownload(data, `${cipher}-qr.png`);
  };

  const downloadSvg = () => {
    if (!svg) return;
    const url = URL.createObjectURL(new Blob([svg], { type: "image/svg+xml" }));
    triggerDownload(url, `${cipher}-qr.svg`);
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  };

  return (
    <div className="hairline rounded-md bg-surface px-5 py-4">
      <p className="eyebrow text-brass-500">Cipher</p>
      <p className="mt-2 font-serif text-2xl tabular-nums">{cipher}</p>
      <div className="mt-4 grid place-items-center rounded-md bg-card p-3 hairline">
        <canvas
          ref={canvasRef}
          aria-label={`QR cipher for ${eventName}`}
          width={256}
          height={256}
          className="h-auto w-full max-w-[220px]"
        />
      </div>
      <div className="mt-3 flex gap-2">
        <Button variant="outline" size="sm" onClick={downloadPng}>
          <Download className="mr-2 h-4 w-4" /> PNG
        </Button>
        <Button variant="outline" size="sm" onClick={downloadSvg}>
          <Download className="mr-2 h-4 w-4" /> SVG
        </Button>
      </div>
      <p className="mt-2 text-xs text-foreground/60">
        Print on event badges. Encodes the plaintext cipher; participants can also type it.
      </p>
    </div>
  );
}
