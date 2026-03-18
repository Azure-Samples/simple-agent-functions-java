FROM mcr.microsoft.com/devcontainers/java:17-bookworm

# Copy custom first notice message.
COPY first-run-notice.txt /tmp/staging/
RUN sudo mv -f /tmp/staging/first-run-notice.txt /usr/local/etc/vscode-dev-containers/ \
    && sudo rm -rf /tmp/staging

# Install Azure Functions Core Tools
RUN curl https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > microsoft.gpg \
    && sudo mv microsoft.gpg /etc/apt/trusted.gpg.d/microsoft.gpg \
    && sudo sh -c 'echo "deb [arch=amd64] https://packages.microsoft.com/debian/$(lsb_release -rs | cut -d'.' -f 1)/prod $(lsb_release -cs) main" > /etc/apt/sources.list.d/dotnetdev.list' \
    && sudo apt-get update \
    && sudo apt-get install -y azure-functions-core-tools-4

# Install Node.js for azurite
RUN sudo apt-get install -y nodejs npm \
    && sudo npm install -g azurite

# Install Azure Developer CLI
RUN curl -fsSL https://aka.ms/install-azd.sh | bash
