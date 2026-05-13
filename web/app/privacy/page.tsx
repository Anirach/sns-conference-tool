import { AppShell } from "@/components/layout/AppShell";

export const metadata = {
  title: "Privacy — SNS"
};

export default function PrivacyPage() {
  return (
    <AppShell title="Privacy" eyebrow="Policy" showBack>
      <article className="prose prose-stone flex-1 space-y-6 px-5 pt-5 pb-12 font-serif text-sm leading-relaxed text-foreground">
        <header className="hairline-b pb-4">
          <p className="eyebrow text-brass-500">In Residence</p>
          <h1 className="mt-2 font-serif text-2xl text-foreground">
            What we keep, and for how long.
          </h1>
        </header>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Identity
          </h2>
          <p className="mt-2">
            You sign up with an email and a password. Passwords are stored as BCrypt-12 hashes;
            we never see the plaintext. Sessions use rotating refresh tokens — if a refresh token
            is reused after rotation, the entire token family is revoked and you’re signed out of
            every device until you log back in.
          </p>
        </section>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Location
          </h2>
          <p className="mt-2">
            Your latitude and longitude are written to your <em>participation</em> row in the
            session you joined, only when you opt in. Positions older than five minutes are
            ignored by the vicinity engine, and your row is deleted when the session adjourns.
            Coordinates never leave the backend.
          </p>
        </section>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Research interests
          </h2>
          <p className="mt-2">
            Each interest you add is processed by a keyword extractor to produce a small vector
            of normalised weights. Cosine similarity between vectors is what surfaces fellows in
            the proximity list. The original text and the vector are stored against your user
            row; nothing is shared with third parties.
          </p>
        </section>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Linked societies
          </h2>
          <p className="mt-2">
            When you link a Facebook or LinkedIn account, we store an AES-256-GCM-encrypted
            OAuth token and the provider’s user-ID. We use it once on link to pre-fill your
            profile, and never again unless you ask us to refresh. Unlinking deletes the
            encrypted blob; we do not retain a copy.
          </p>
        </section>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Correspondence
          </h2>
          <p className="mt-2">
            Chat messages are scoped to a single session and are visible only to you and your
            chosen fellow. They’re removed when the session’s data is purged or when either
            party deletes their account.
          </p>
        </section>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Audit
          </h2>
          <p className="mt-2">
            Every action you take that touches another user (sending a chat, joining a session,
            changing a setting) writes an audit entry. The audit log is append-only at the
            database level, scrubbed of email addresses and tokens, and pruned after 180 days.
            IP addresses are hashed before persistence.
          </p>
        </section>

        <section>
          <h2 className="font-serif text-base font-semibold uppercase tracking-[0.14em] text-brass-500">
            Export & delete
          </h2>
          <p className="mt-2">
            Tap <em>Export my dossier</em> on the Study tab to download a ZIP containing every
            byte we hold about you: profile, interests, matches, chats, linked societies. Tap{" "}
            <em>Delete my account</em> for a soft delete. Your row is purged thirty days later;
            until then it can be restored on request.
          </p>
        </section>

        <p className="hairline-t pt-4 font-serif text-xs italic text-muted-foreground">
          Questions? Email <a href="mailto:privacy@sns.example.com" className="underline">privacy@sns.example.com</a>.
        </p>
      </article>
    </AppShell>
  );
}
