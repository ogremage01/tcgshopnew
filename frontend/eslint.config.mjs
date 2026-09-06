import nextConfig from "eslint-config-next";

/** @type {import("eslint").Linter.FlatConfig[]} */
const config = [
  ...(Array.isArray(nextConfig) ? nextConfig : [nextConfig]),
  {
    rules: {
      // Keep React Compiler checks visible without failing CI/build immediately.
      "react-hooks/set-state-in-effect": "warn",
      "react-hooks/purity": "warn",
      "react-hooks/incompatible-library": "warn",
    },
  },
  { ignores: ["**/node_modules/**", "**/.next/**", "**/out/**", "**/dist/**", "**/build/**"] },
];

export default config;
