#
#  ci_post_clone.sh
#  ReaderIOS
#
#  Created by Jonas Schiessl on 2/1/25.
#

#!/bin/sh

# Print start of setup
echo "Starting CI setup..."

defaults write com.apple.dt.Xcode IDESkipPackagePluginFingerprintValidatation -bool YES
defaults write com.apple.dt.Xcode IDESkipMacroFingerprintValidation -bool YES

