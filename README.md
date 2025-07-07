# Fireporter: Firefly III PDF Exporter

![fireporter-38.png](src/main/resources/com/fadlan/fireporter/fireporter-38.png)

Fireporter is a third-party app for Firefly III, it uses Firefly's API to download your financial records and process it to a PDF file.

## Requirements
- Firefly III version 6.x.x
- Personal Access Token (PAT)
- Java version 17 or higher

## Usage

1. Enter the address to access your Firefly III installation.
2. Enter a Personal Access Token from your Firefly III account. You can create one on `Options -> Provile -> OAuth` page.
3. Choose the period you want to report. 
4. Click `Generate` to create the PDF.

## TODO

- [ ] Build to executable for distribution
- [x] Toggle attachments
- [ ] Multi-currency support (Major Update)

## Important Notes
- Currently, supports only Indonesian Rupiah (IDR). Multi-currency support is planned.
- Tested only on Windows 10. Should work on Linux and macOS, but compatibility isn't guaranteed.
- Fireporter only reads data from Firefly III via API and formats it into a PDF.

## Disclaimer
Fireporter is provided "as is", without warranty of any kind. The developer is not responsible for any loss, damage, or errors caused by use of this application. Use at your own risk.

## License
This project is licensed under the [GNU AGPL v3.0](LICENSE).