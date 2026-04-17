import Link from "next/link";
import { Sparkles, Users, MapPin, MessageCircle } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default function WelcomePage() {
  return (
    <div className="flex min-h-dvh flex-col bg-gradient-to-b from-brand-50 via-white to-white">
      <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col justify-between px-6 pb-10 pt-[calc(env(safe-area-inset-top)+3rem)]">
        <div className="flex flex-col gap-6">
          <div className="flex items-center gap-2 text-brand-700">
            <Sparkles className="h-5 w-5" />
            <span className="text-sm font-medium uppercase tracking-wide">SNS Conference Tool</span>
          </div>
          <h1 className="text-4xl font-semibold leading-tight text-gray-900">
            Find the people you came to meet.
          </h1>
          <p className="text-base text-gray-600">
            At on-site conferences, discover nearby researchers who share your interests, and start a real
            conversation — in person or in the app.
          </p>

          <ul className="mt-4 flex flex-col gap-4">
            <Feature icon={Users} title="Shared interests">
              We match you by research topics extracted from your own bio, articles, and links.
            </Feature>
            <Feature icon={MapPin} title="On-site proximity">
              See matches within 20, 50, or 100 metres during the event.
            </Feature>
            <Feature icon={MessageCircle} title="Private chat">
              Message people at the venue. Data deletes automatically when the event ends.
            </Feature>
          </ul>
        </div>

        <div className="mt-10 flex flex-col gap-3">
          <Link href="/register">
            <Button size="lg" fullWidth>
              Create account
            </Button>
          </Link>
          <Link href="/login">
            <Button size="lg" variant="secondary" fullWidth>
              I already have an account
            </Button>
          </Link>
        </div>
      </main>
    </div>
  );
}

function Feature({
  icon: Icon,
  title,
  children
}: {
  icon: typeof Sparkles;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <li className="flex gap-3">
      <div className="mt-0.5 rounded-lg bg-brand-50 p-2 text-brand-600">
        <Icon className="h-5 w-5" />
      </div>
      <div>
        <div className="font-semibold text-gray-900">{title}</div>
        <div className="text-sm text-gray-600">{children}</div>
      </div>
    </li>
  );
}
