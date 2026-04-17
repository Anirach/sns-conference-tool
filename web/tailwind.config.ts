import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./lib/**/*.{ts,tsx}"
  ],
  theme: {
    container: { center: true, padding: "1rem", screens: { "2xl": "1400px" } },
    extend: {
      fontFamily: {
        sans: ["Instrument Sans", "system-ui", "sans-serif"],
        serif: ["Playfair Display", "Georgia", "serif"],
        display: ["Playfair Display", "Georgia", "serif"]
      },
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))"
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))"
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))"
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))"
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
          500: "hsl(var(--accent-500))"
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))"
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))"
        },
        brand: {
          50: "hsl(var(--brand-50))",
          100: "hsl(var(--brand-100))",
          200: "hsl(var(--brand-200))",
          300: "hsl(var(--brand-300))",
          400: "hsl(var(--brand-400))",
          500: "hsl(var(--brand-500))",
          600: "hsl(var(--brand-600))",
          700: "hsl(var(--brand-700))",
          800: "hsl(var(--brand-800))",
          900: "hsl(var(--brand-900))"
        },
        brass: {
          50: "hsl(var(--brass-50))",
          100: "hsl(var(--brass-100))",
          300: "hsl(var(--brass-300))",
          500: "hsl(var(--brass-500))",
          600: "hsl(var(--brass-600))",
          700: "hsl(var(--brass-700))"
        },
        success: { DEFAULT: "hsl(var(--success))" },
        warning: { DEFAULT: "hsl(var(--warning))" },
        danger: { DEFAULT: "hsl(var(--danger))" },
        surface: {
          DEFAULT: "hsl(var(--surface))",
          muted: "hsl(var(--surface-muted))",
          sunken: "hsl(var(--surface-sunken))"
        }
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 4px)",
        sm: "calc(var(--radius) - 8px)"
      },
      keyframes: {
        "laser-scan": {
          "0%,100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(180px)" }
        },
        "pulse-halo": {
          "0%": { boxShadow: "0 0 0 0 hsl(var(--success) / 0.55)" },
          "70%": { boxShadow: "0 0 0 6px hsl(var(--success) / 0)" },
          "100%": { boxShadow: "0 0 0 0 hsl(var(--success) / 0)" }
        },
        "fade-in-up": {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" }
        },
        "typing-bounce": {
          "0%,100%": { transform: "translateY(0)", opacity: "0.5" },
          "50%": { transform: "translateY(-4px)", opacity: "1" }
        }
      },
      animation: {
        "laser-scan": "laser-scan 2.4s ease-in-out infinite",
        "pulse-halo": "pulse-halo 1.6s ease-out infinite",
        "fade-in-up": "fade-in-up 0.25s ease-out both",
        "typing-bounce": "typing-bounce 0.9s ease-in-out infinite"
      }
    }
  },
  plugins: []
};

export default config;
