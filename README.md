# Fireporter: Firefly III PDF Exporter

![fireporter-38.png](src/main/resources/com/fadlan/fireporter/fireporter-38.png)

Fireporter is a third-party app for Firefly III, it uses Firefly's API to download your financial records and process it to a PDF file.

## 🚀 Releases

Download the latest version here: [Fireporter Latest Version](https://github.com/vFadlan011/fireporter/releases/latest)

## 📋 Requirements
- Firefly III version 6.x.x
- Personal Access Token (PAT)

## 🛠️ Usage

1. Enter the address to access your Firefly III installation.
2. Enter a Personal Access Token from your Firefly III account. You can create one on `Options -> Profile -> OAuth` page.
3. Choose the period you want to report.
4. Click `Generate` to create the PDF.

## ✅ TODO

- [ ] Github Action for building distributions
- [ ] Budget
- [ ] Add Logging
- [ ] Multi-currency support (Major Update)
- [x] <s>Toggle attachments</s>
- [x] <s>Build to executable for distribution</s>

## ⚠️ Important Notes
- Currently, supports only Indonesian Rupiah (IDR). Multi-currency support is planned.
- Release only available for Windows 10, others will be added.
- Fireporter only reads data from Firefly III via API and formats it into a PDF.

## 📄 Disclaimer
Fireporter is provided "as is", without warranty of any kind. The developer is not responsible for any loss, damage, or errors caused by use of this application. Use at your own risk.

## 📜 License
This project is licensed under the [GNU AGPL v3.0](LICENSE).

## 📌 Changelog
### 0.39.0
- Update java version 17 to java 21
- Update gradle version 8.2 to 8.10
- Change packaging plugin from org.beryx.jlink to com.dua3.gradle.runtime
- Write (working) packaging task for windows with `badass-runtime`
- Write untested packaging task for macos and linux