# 手机 GitHub Codespaces 更新到 v1.5.1 FINAL

把 `CuotiPrint-Android-v1.5.1-FINAL-VERIFIED-source.zip` 上传到现有仓库根目录并 Commit，然后在 Codespaces 终端执行：

```bash
cd /workspaces/-
rm -rf /tmp/cuoti151
mkdir -p /tmp/cuoti151
unzip -q CuotiPrint-Android-v1.5.1-FINAL-VERIFIED-source.zip -d /tmp/cuoti151
cp -a /tmp/cuoti151/QrintPrint-Android-v1.5.1-FINAL-VERIFIED/. /workspaces/-/
rm -f CuotiPrint-Android-v1.5.1-FINAL-VERIFIED-source.zip
git add -A
git commit -m "CuotiPrint Android v1.5.1 final verified"
git push
```

随后打开 GitHub → Actions。成功时 Artifact 名称为：

`CuotiPrint-Android-v1.5.1-final-verified-debug`
