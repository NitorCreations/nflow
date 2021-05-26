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
- proper testing
- Integrate with nflow deployment/build
  - requires http server support: (return index.html on 404)
- config.json file
- config.js with code injection, some solution needed?
- redirect old style #! urls to new urls
- Modify workflow instance
- Create new workflow instance via UI
