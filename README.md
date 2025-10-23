# Git Setup Notes

This repository now contains the full stack project (Flask + Spring Boot + Vue) in its original structure. A workspace-level `.gitignore` keeps heavy assets (datasets, weights, build outputs, `node_modules`, etc.) out of version control, so only the core source code is tracked.

## Initialize the Repository

Run the commands from the project root (`yolo_cropDisease_detection_web/`):

```bash
git init

# (Optional) Configure your author information
git config user.name "Your Name"
git config user.email "you@example.com"

# Review which files are tracked after the ignore rules
git status

# Stage the core source modules
git add yolo_cropDisease_detection_flask yolo_cropDisease_detection_springboot yolo_cropDisease_detection_vue .gitignore README.md

# Double-check the staging area
git status

# Create the initial commit
git commit -m "Initialize crop disease detection platform core code"

# (Optional) add remote and push
git remote add origin <your-remote-url>
git branch -M main
git push -u origin main
```

## What Stays Local

- Large datasets, model weights, YOLO runs, generated prediction results.
- Spring Boot build artifacts under `target/`.
- Vue’s `node_modules/` and build outputs under `dist/`.
- Editor metadata (`.vscode/`, `.idea/`), logs, temporary caches.

Add these back later in separate commits if you need them in Git, or adjust `.gitignore` before staging.***
