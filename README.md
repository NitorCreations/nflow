# nFlow explorer redux

The old UI is at http://bank.nflow.io/nflow/ui/explorer/#!/

## Development

Installation
```
npm install
```
Starting dev server
```
npm start
```

Point your browser to http://localhost:3000/

Use `npm`. `yarn` has caused problems.

### Auto formatting

This repository has code auto formatting enabled. Auto formatter runs before commit.

## TODO
- Testing and automated tests
- Integrate with nflow deployment/build
  - Requires http server support: (return index.html on 404)
- Layout
- Configuration
  - config.json file
  - config.js with code injection, some solution needed?
- Compatibility with the old
  - redirect old style #! urls to new urls
- State graph
  - highlight
  - Show workflow instance data
- Modify existing workflow instance
- Create new workflow instance via UI
