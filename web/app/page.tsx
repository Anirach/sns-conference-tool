import Link from "next/link";

export default function WelcomePage() {
  return (
    <div className="mobile-frame flex flex-col bg-background px-8 pb-10 pt-16">
      <header className="pb-8 text-center hairline-b">
        <p className="eyebrow text-brass-500">Vol. IV — Established MMXXIV</p>
        <h1 className="mt-3 font-serif text-5xl leading-[1.05] text-foreground">
          Intellectual
          <br />
          <span className="italic">Affinities.</span>
        </h1>
      </header>

      <div className="-mt-8 flex flex-1 flex-col justify-center px-2 text-center">
        <p className="font-serif text-xl italic leading-relaxed text-foreground/80">
          &ldquo;The pursuit of shared knowledge is the highest form of collegiality.&rdquo;
        </p>
        <p className="mx-auto mt-6 max-w-[28ch] text-xs leading-relaxed text-muted-foreground">
          A discreet register connecting researchers at conferences through curated affinities of inquiry.
        </p>
      </div>

      <div className="space-y-4 pt-8 hairline-t">
        <Link
          href="/register"
          className="inline-flex h-12 w-full items-center justify-center gap-2 rounded-sm bg-brand-500 px-6 text-sm font-semibold uppercase tracking-[0.18em] text-background transition-colors hover:bg-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brass-500 focus-visible:ring-offset-2 focus-visible:ring-offset-background"
        >
          Request Admission
        </Link>
        <Link
          href="/login"
          className="block py-2 text-center font-serif text-sm italic text-brass-600 hover:text-brass-700"
        >
          A returning fellow
        </Link>
      </div>

      <p className="eyebrow mt-6 text-center text-foreground/30">Curated Academic Registry</p>
    </div>
  );
}
