# 📦 How to Release Linid Identity Manager

This document explains the full process for creating a new release of **Linid Identity Manager**.

Follow these steps carefully to ensure consistency across all repositories and submodules.

---

## 0️⃣ Create a Release Branch

Before updating submodules, create a **release branch** from `main`:

```bash
git checkout main
git pull
git checkout -b release/vX.X.X
git push origin release/vX.X.X
```

This branch will be used to prepare the release before creating the stable branch.

## 1️⃣ Update Submodules

Update every submodule to the latest available **tag**:

```bash
git submodule foreach "git fetch --tags"
git submodule foreach "git checkout <latest-tag>"
```

Verify:

```bash
git submodule foreach "git describe --tags"
```

Then **update the README** to ensure the **submodules list and their associated tags** are correct and reflect the newly
selected versions.

Make sure the section documenting submodules now references the updated tags.

---

## 2️⃣ Update the Changelog

Edit `CHANGELOG.md`:

- Add a new entry for the upcoming version
- Document all changes (features, fixes, improvements)

---

## 3️⃣ Create a Pull Request

Create a PR on GitHub with:

- Updated submodules
- Updated changelog
- Release description

Wait for approval and merging.

---

## 4️⃣ Create a Stable Branch for the Release

Before tagging, create a new release branch:

```bash
git checkout main
git pull
git checkout -b stable/vX.X.X
git push origin stable/vX.X.X
```

This branch will represent the frozen, stable release.

---

## 5️⃣ Set the Stable Branch as the Default Branch on GitHub

Go to:

**GitHub → Settings → Branches → Default branch**

Select:

```
stable/vX.X.X
```

This ensures:

- New users land on the correct stable version
- The code displayed on GitHub matches the release
- Future development can continue on `main` without affecting the stable version

---

## 6️⃣ Tag the Release

From the stable branch:

```bash
git checkout stable/vX.X.X
git pull
git tag vX.X.X
git push origin vX.X.X
```

The tag now identifies the exact version.

---

## 7️⃣ Create the GitHub Release

On GitHub → **Releases**:

- Click **“Draft a new release”**
- Choose the tag you just pushed
- Add release notes (you can reuse the changelog section)
- Publish

---

## 8️⃣ Update Submodules Back to `main`

After the release is finished:

```bash
git checkout main
git pull
git submodule foreach "git checkout main"
git submodule update --remote
```

Then commit:

```bash
git add .
git commit -m "Update submodules to latest main after release"
git push
```

Create a PR with these updates.
