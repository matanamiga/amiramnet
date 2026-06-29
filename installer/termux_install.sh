#!/data/data/com.termux/files/usr/bin/bash
#
# Phantom — one-line Termux installer for the phone brain.
#
#   curl -fsSL https://raw.githubusercontent.com/matanamiga/amiramnet/main/installer/termux_install.sh | bash
#
# Installs Python, Ollama and a vision model, fetches Phantom, and drops a
# `phantom` launcher command. Safe to re-run (idempotent).
set -euo pipefail

REPO_URL="${PHANTOM_REPO_URL:-https://github.com/matanamiga/amiramnet}"
INSTALL_DIR="${PHANTOM_HOME:-$HOME/phantom-app}"
MODEL="${PHANTOM_MODEL:-qwen2.5-vl:7b}"
BIN="$PREFIX/bin/phantom"

say() { printf '\n\033[1;35m👻 %s\033[0m\n' "$*"; }

say "Updating Termux packages…"
pkg update -y && pkg upgrade -y
pkg install -y python git ollama

say "Installing Python dependencies…"
pip install --upgrade pip
pip install requests pydantic

say "Fetching Phantom from $REPO_URL…"
if [ -d "$INSTALL_DIR/.git" ]; then
  git -C "$INSTALL_DIR" pull --ff-only
else
  git clone --depth 1 "$REPO_URL" "$INSTALL_DIR"
fi

say "Starting Ollama and pulling the vision model ($MODEL)…"
if ! pgrep -f "ollama serve" >/dev/null 2>&1; then
  nohup ollama serve >"$HOME/.phantom-ollama.log" 2>&1 &
  sleep 3
fi
ollama pull "$MODEL"

say "Installing the 'phantom' launcher…"
cat > "$BIN" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
# Phantom launcher — runs the phone brain.
export PHANTOM_MODEL="\${PHANTOM_MODEL:-$MODEL}"
if [ -z "\${PHANTOM_PC_URL:-}" ]; then
  echo "Set PHANTOM_PC_URL to the PC gate URL (it is printed when you start Phantom-Gate on the PC)."
  echo "  example: export PHANTOM_PC_URL=http://192.168.1.50:8765"
  exit 1
fi
pgrep -f "ollama serve" >/dev/null 2>&1 || { nohup ollama serve >"\$HOME/.phantom-ollama.log" 2>&1 & sleep 3; }
termux-wake-lock 2>/dev/null || true
cd "$INSTALL_DIR"
exec python -m phantom.phone "\$@"
EOF
chmod +x "$BIN"

say "Done!  Next steps:"
cat <<EOF

  1) On your PC, run Phantom-Gate and copy the URL it prints.
  2) Here in Termux:
        export PHANTOM_PC_URL=http://<that-url>
        phantom "open the calculator and compute 12 * 9"

  Tip: keep the phone awake — battery set to Unrestricted for Termux.
EOF
