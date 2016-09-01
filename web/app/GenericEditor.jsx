import React from 'react'
import R from 'ramda'
import { modelData, modelTitle, modelEmpty, modelItems } from './EditorModel.js'
import { opiskeluOikeusChange } from './Oppija.jsx'
import { formatISODate, parseFinnishDate } from './date.js'
import Http from './http'
import Bacon from 'baconjs'
import { hasClass, addClass, removeClass } from './classnames'

export const Editor = React.createClass({
  render() {
    let { model, context } = this.props
    return getModelEditor(model, context)
  }
})

export const ObjectEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let className = model.value
      ? 'object ' + model.value.class
      : 'object empty'
    let representative = findRepresentative(model)
    let representativeEditor = () => getModelEditor(representative.model, childContext(context, representative.key))
    let objectEditor = () => <div className={className}><PropertiesEditor properties={model.value.properties}
                                                                          context={context}/></div>

    let exactlyOneVisibleProperty = model.value.properties.filter(shouldShowProperty(context.edit)).length == 1
    let isInline = ObjectEditor.canShowInline(model, context)
    let objectWrapperClass = 'foldable-wrapper with-representative' + (isInline ? ' inline' : '')

    return !representative
      ? objectEditor()
      : (exactlyOneVisibleProperty && !context.edit)
        ? representativeEditor() // just show the representative property, no need for FoldableEditor
        : isArrayItem(context) // for array item, show representative property in expanded view too
          ? (<span className={objectWrapperClass}>
              <span className="representative">{representativeEditor()}</span>
              <FoldableEditor expandedView={objectEditor} defaultExpanded={context.edit}/>
            </span>)
          : (<span className={objectWrapperClass}>
              <FoldableEditor expandedView={objectEditor} collapsedView={representativeEditor} defaultExpandeded={context.edit}/>
            </span>)
  }
})
ObjectEditor.canShowInline = (model, context) => !!findRepresentative(model) && !context.edit && !isArrayItem(context)

export const FoldableEditor = React.createClass({
  render() {
    let {collapsedView, expandedView, defaultExpanded} = this.props
    var expanded = this.state? this.state.expanded : defaultExpanded
    let toggleExpanded = () => {
      expanded = !expanded
      function resetSimple(node) {
        if (expanded && hasClass(node, 'inline')) {
          removeClass(node, 'inline')
          addClass(node, 'inline-when-collapsed')
        } else if (!expanded && hasClass(node, 'inline-when-collapsed')) {
          removeClass(node, 'inline-when-collapsed')
          addClass(node, 'inline')
        }
        if (node.parentNode) resetSimple(node.parentNode)
      }
      resetSimple(this.refs.foldable)

      this.setState({expanded})
    }
    let className = expanded ? 'foldable expanded' : 'foldable collapsed'
    return (<span ref="foldable" className={className}>
      <a className="toggle-expand" onClick={toggleExpanded}>{ expanded ? '-' : '+' }</a>
      { expanded ? expandedView() : (collapsedView ? collapsedView() : null) }
    </span>)
  }
})
FoldableEditor.canShowInline = () => true

export const PropertiesEditor = React.createClass({
  render() {
    let {properties, context} = this.props
    let edit = context.edit || (this.state && this.state.edit)
    let toggleEdit = () => this.setState({edit: !edit})
    let shouldShow = shouldShowProperty(edit)
    return (<ul className="properties">
      {
        context.editable && !context.edit ? <a className="toggle-edit" onClick={toggleEdit}>{edit ? 'valmis' : 'muokkaa'}</a> : null
      }
      {
        properties.filter(shouldShow).map(property => {
          let propertyClassName = 'property ' + property.key
          return (<li className={propertyClassName} key={property.key}>
            <label>{property.title}</label>
            <span className="value">{ getModelEditor(property.model, childContext(R.merge(context, {edit: edit}), property.key)) }</span>
          </li>)
        })
      }
    </ul>)
  }
})
PropertiesEditor.canShowInline = () => false

const shouldShowProperty = (edit) => (property) => (edit || !modelEmpty(property.model)) && !property.hidden

export const ArrayEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let items = modelItems(model)
    let inline = ArrayEditor.canShowInline(model, context)
    let className = inline ? 'array inline' : 'array'
    let adding = this.state && this.state.adding || []
    let add = () => this.setState({adding: adding.concat(model.prototype)})
    return (
      <ul ref="ul" className={className}>
        {
          items.concat(adding).map((item, i) =>
            <li key={i}>{getModelEditor(item, R.merge(childContext(context, i), { arrayItems: items }) )}</li>
          )
        }
        {
          context.edit && model.prototype !== undefined ? <li className="add-item"><a onClick={add}>lisää uusi</a></li> : null
        }
      </ul>
    )
  }
})
ArrayEditor.canShowInline = (model, context) => {
  var items = modelItems(model)
  return items.length <= 1 && canShowInline(items[0], childContext(context, 0))
}

export const OptionalEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let adding = this.state && this.state.adding
    let add = () => this.setState({adding: true})
    return adding
      ? getModelEditor(model.prototype, context, true)
      : <a className="add-value" onClick={add}>lisää</a>
  }
})
OptionalEditor.canShowInline = () => true

export const StringEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let {valueBus} = this.state

    let onChange = (event) => {
      valueBus.push([context, {data: event.target.value}])
    }

    return context.edit
      ? <input type="text" defaultValue={modelData(model)} onChange={ onChange }></input>
      : <span className="inline string">{modelData(model)}</span>
  },

  getInitialState() {
    return {valueBus: Bacon.Bus()}
  },

  componentDidMount() {
    this.state.valueBus.throttle(1000).onValue((v) => {opiskeluOikeusChange.push(v)})
  }
})
StringEditor.canShowInline = () => true

export const BooleanEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let onChange = event => {
      opiskeluOikeusChange.push([context, {data: event.target.checked}])
    }

    return context.edit
      ? <input type="checkbox" defaultChecked={modelData(model)} onChange={ onChange }></input>
      : <span className="inline string">{modelTitle(model)}</span>
  }
})
BooleanEditor.canShowInline = () => true

export const DateEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let {invalidDate, valueBus} = this.state

    let onChange = (event) => {
      var date = parseFinnishDate(event.target.value)
      if (date) {
        valueBus.push([context, {data: formatISODate(date)}])
      }
      this.setState({invalidDate: date ? false : true})
    }

    return context.edit
      ? <input type="text" defaultValue={modelTitle(model)} onChange={ onChange } className={invalidDate ? 'error' : ''}></input>
      : <span className="inline date">{modelTitle(model)}</span>
  },

  getInitialState() {
    return {valueBus: Bacon.Bus()}
  },

  componentDidMount() {
    this.state.valueBus.throttle(1000).onValue((v) => {opiskeluOikeusChange.push(v)})
  }
})
DateEditor.canShowInline = () => true

export const EnumEditor = React.createClass({
  render() {
    let {model, context} = this.props
    let alternatives = model.alternatives || (this.state.alternatives) || []
    let className = alternatives.length ? '' : 'loading'
    let onChange = (event) => {
      let selected = alternatives.find(alternative => alternative.value == event.target.value)
      opiskeluOikeusChange.push([context, selected])
    }
    return context.edit
      ? (<select className={className} defaultValue={model.value && model.value.value} onChange={ onChange }>
      {
        alternatives.map( alternative =>
          <option value={ alternative.value } key={ alternative.value }>{alternative.title}</option>
        )
      }
    </select>)
      : <span className="inline enum">{modelTitle(model)}</span>
  },

  update(props) {
    let {model, context} = props
    if (context.edit && model.alternativesPath && !this.state.alternativesP) {
      this.state.alternativesP = EnumEditor.AlternativesCache[model.alternativesPath]
      if (!this.state.alternativesP) {
        this.state.alternativesP = Http.get(model.alternativesPath).toProperty()
        EnumEditor.AlternativesCache[model.alternativesPath] = this.state.alternativesP
      }
      this.state.alternativesP.onValue(alternatives => this.setState({alternatives}))
    }
  },

  componentWillMount() {
    this.update(this.props)
  },

  componentWillReceiveProps(props) {
    this.update(props)
  },

  getInitialState() {
    return {}
  }
})
EnumEditor.canShowInline = () => true
EnumEditor.AlternativesCache = {}

export const NullEditor = React.createClass({
  render() {
    return null
  }
})

export const childContext = (context, ...pathElems) => {
  let path = ((context.path && [context.path]) || []).concat(pathElems).join('.')
  return R.merge(context, { path, root: false, arrayItems: null })
}

export const rootContext = (rootModel, editorMapping) => ({ root: true, prototypes: rootModel.prototypes, editorMapping: R.merge(defaultEditorMapping, editorMapping) })

const findRepresentative = (model) => model.value.properties.find(property => property.representative)
const isArrayItem = (context) => context.arrayItems && context.arrayItems.length > 1
const canShowInline = (model, context) => (getEditorFunction(model, context).canShowInline || (() => false))(model, context)

const resolveModel = (model, context) => {
  if (model && model.type == 'prototype' && context.editable) {
    let prototypeModel = context.prototypes[model.key]
    model = model.optional
      ? R.merge(prototypeModel, { value: null, optional: true, prototype: model.prototype}) // Remove value from prototypal value of optional model, to show it as empty
      : prototypeModel
  }
  return model
}

const getEditorFunction = (model, context) => {
  model = resolveModel(model, context)
  if (!model) return NullEditor
  if (modelEmpty(model) && model.optional && model.prototype !== undefined) {
    return OptionalEditor
  }
  let editor = (model.value && context.editorMapping[model.value.class]) || context.editorMapping[model.type]
  if (!editor) {
    if (!model.type) {
      console.log('Typeless model', model)
    }
    console.log('Missing editor ' + model.type)
    return NullEditor
  }
  return editor
}

const getModelEditor = (model, context) => {
  model = resolveModel(model, context)
  var ModelEditor = getEditorFunction(model, context)
  return <ModelEditor model={model} context={context} />
}

const defaultEditorMapping = {
  'object': ObjectEditor,
  'array': ArrayEditor,
  'string': StringEditor,
  'number': StringEditor,
  'date': DateEditor,
  'boolean': BooleanEditor,
  'enum': EnumEditor
}