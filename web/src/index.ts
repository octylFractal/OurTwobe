async function initialize() {
    await import(/* webpackPreload: true */ "./bootstrap");
}

initialize().catch(err => console.error(err));
