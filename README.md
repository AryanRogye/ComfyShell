# ComfyShell

> 🛠️ A lightweight FireTV app that enables reverse shell access to your FireTV device using raw Java. Built for control, speed, and simplicity.

---

### 💡 What It Does

ComfyShell lets you easily establish a reverse shell connection from your FireTV to your computer. Once set up, your FireTV remembers the IP and port so you can quickly connect back without needing to reconfigure anything.

> 🔄 Perfect for tinkering, debugging, or running commands remotely from your machine.

---

### 🚀 How It Works

- ComfyShell connects to your machine using your IP and a port (default: `4444`)
- You can change this port by editing `ReverseShellManager.java`
- Just run a listener on your computer:

```bash
nc -lvn 4444
