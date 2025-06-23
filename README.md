# ComfyShell

> 🛠️ A lightweight FireTV app that enables reverse shell access to your FireTV device using raw Java. Built for control, speed, and simplicity.

---

### 💡 What It Does

ComfyShell lets you easily establish a reverse shell connection from your FireTV to your computer. Once set up, your FireTV remembers the IP and port, so you can reconnect quickly without needing to reconfigure anything.

It also supports **APK installation** — though this feature hasn’t been fully expanded yet due to lack of a use case. It’s there if you need it.

> 🔄 Perfect for tinkering, debugging, or running commands remotely from your machine.

---

### 🚀 How It Works

- ComfyShell connects to your machine using your IP and a port (default: `4444`)
- You can change this port by editing `ReverseShellManager.java`
- On your machine, run a listener:

```bash
nc -lvn 4444
```

- Once the app launches, it’ll send a shell back to your listener. That’s it — you’re in

### ⚠️ Disclaimer

- This tool is for educational and personal use only.
- Do not use it to access devices you do not own or have explicit permission to control.
- The developer is not responsible for any misuse or consequences.
