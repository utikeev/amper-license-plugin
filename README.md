# Amper License Plugin

The plugin heavily inspired by [License Maven plugin](https://www.mojohaus.org/license-maven-plugin/).

## Tasks

- `listLicenses` — lists popular known licenses.
- `checkFileHeaders` — checks whether sources (only production ones) have copyright headers.
- `updateFileHeaders` — updates copyright headers in sources (only production ones).
- `removeFileHeaders` — removes copyright headers from sources (only production ones).
- `collectThirdParty` — collects third party licenses in a report for module.

## Configuration

The plugin supports the following configuration options:

- `detailedInfo` — whether to print content of licenses in `listLicenses` task.
- `headerSettings` — settings for the copyright header related tasks.
  - `inceptionYear` — year of the project inception.
  - `licenseName` — name of the license to use (see `license-plugin/resources` for the acceptable names).
  - `organizationName` — name of the organization owning the module.
  - `onMissingHeader` — `fail` or `warn`. Former will fail the `test` task, while the latter will just print a warning into the log. 
