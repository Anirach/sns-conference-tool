"use client";

import { ReactNode } from "react";
import { AppBar } from "./AppBar";
import { BottomTabBar } from "./BottomTabBar";

interface AppShellProps {
  title?: string;
  eyebrow?: string;
  subtitle?: string;
  showBack?: boolean;
  right?: ReactNode;
  hideTabs?: boolean;
  hideAppBar?: boolean;
  children: ReactNode;
}

export function AppShell({
  title,
  eyebrow,
  subtitle,
  showBack,
  right,
  hideTabs,
  hideAppBar,
  children
}: AppShellProps) {
  return (
    <div className="mobile-frame flex flex-col">
      {!hideAppBar && title ? (
        <AppBar title={title} eyebrow={eyebrow ?? subtitle} showBack={showBack} trailing={right} />
      ) : null}
      <main className="flex flex-1 flex-col">{children}</main>
      {!hideTabs ? <BottomTabBar /> : null}
    </div>
  );
}
