import type { MetadataRoute } from "next";

import { ROBOTS_DISALLOW_PATHS } from "@/lib/sitemap/robots-disallow";
import { getSiteUrl } from "@/lib/sitemap/site-url";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [...ROBOTS_DISALLOW_PATHS],
    },
    sitemap: `${getSiteUrl()}/sitemap.xml`,
  };
}
