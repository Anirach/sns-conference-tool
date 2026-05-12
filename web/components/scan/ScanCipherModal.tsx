"use client";

import { Html5Qrcode } from "html5-qrcode";
import { Camera } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";

interface ScanCipherModalProps {
  open: boolean;
  onClose: () => void;
  onScan: (cipher: string) => void;
}

const READER_ID = "sns-cipher-reader";

/**
 * Full-screen camera viewfinder for scanning a session cipher QR code. Uses html5-qrcode
 * which abstracts getUserMedia + jsqr-equivalent decoding across iOS Safari 14.5+ and
 * Chrome on Android. The decoded text is whatever the QR encodes — for our event QRs that
 * is just the plaintext cipher (e.g. NEURIPS2026), which the join API already accepts.
 */
export function ScanCipherModal({ open, onClose, onScan }: ScanCipherModalProps) {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    let cancelled = false;

    const start = async () => {
      try {
        const instance = new Html5Qrcode(READER_ID, { verbose: false });
        scannerRef.current = instance;
        await instance.start(
          { facingMode: "environment" },
          { fps: 10, qrbox: { width: 240, height: 240 } },
          (decodedText) => {
            if (cancelled) return;
            cancelled = true;
            onScan(decodedText.trim());
            void instance.stop().catch(() => null);
            onClose();
          },
          () => {
            /* per-frame decode failure — ignore noise */
          }
        );
      } catch (e) {
        setError(e instanceof Error ? e.message : "Camera unavailable");
      }
    };

    void start();
    return () => {
      cancelled = true;
      const inst = scannerRef.current;
      if (inst) {
        void Promise.resolve(inst.stop())
          .catch(() => null)
          .finally(() => Promise.resolve(inst.clear()).catch(() => null));
        scannerRef.current = null;
      }
    };
  }, [open, onClose, onScan]);

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (!o) onClose();
      }}
      eyebrow="Scan Cipher"
      title="Hold the badge in the box"
    >
      <div className="flex flex-col gap-4">
        <div
          id={READER_ID}
          className="aspect-square w-full overflow-hidden rounded-md bg-foreground/90 hairline"
        />
        {error ? (
          <div className="hairline rounded-md bg-surface px-3 py-3 text-xs text-red-700">
            <p className="flex items-center gap-2">
              <Camera className="h-3.5 w-3.5" /> {error}
            </p>
            <p className="mt-1 text-foreground/60">
              Allow camera access in your browser, or close this and type the cipher instead.
            </p>
          </div>
        ) : (
          <p className="text-xs text-foreground/60">
            The cipher will appear on your event badge below the QR.
          </p>
        )}
        <Button variant="outline" size="sm" fullWidth onClick={onClose}>
          Cancel
        </Button>
      </div>
    </Dialog>
  );
}
