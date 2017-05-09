import React from 'baret'
import R from 'ramda'
import * as L from 'partial.lenses'
import Atom from 'bacon.atom'
import {modelData, modelItems, modelLookup} from './EditorModel.js'
import {OpiskeluoikeusjaksoEditor} from './OpiskeluoikeusjaksoEditor.jsx'
import {OpiskeluoikeudenUusiTilaPopup} from './OpiskeluoikeudenUusiTilaPopup.jsx'
import {modelSetValue, lensedModel, pushModel, pushRemoval} from './EditorModel'
import {suoritusKesken} from './Suoritus'
import {parseISODate} from '../date.js'

export const OpiskeluoikeudenTilaEditor = ({model}) => {
  let wrappedModel = lensedModel(model, L.rewrite(fixPäättymispäivä))
  let jaksotModel = opiskeluoikeusjaksot(wrappedModel)
  let addingNew = Atom(false)
  let items = setActive(modelItems(jaksotModel).slice(0).reverse())
  let suorituksiaKesken = wrappedModel.context.edit && R.any(suoritusKesken)(modelItems(wrappedModel, 'suoritukset'))
  let showAddDialog = () => addingNew.modify(x => !x)

  let lisääJakso = (uusiJakso) => {
    if (uusiJakso) {
      pushModel(uusiJakso, wrappedModel.context.changeBus)
    }
    addingNew.set(false)
  }

  let removeItem = () => {
    pushRemoval(items[0], wrappedModel.context.changeBus)
    addingNew.set(false)
  }

  let showLisaaTila = wrappedModel.context.edit && !onLopputilassa(wrappedModel)
  let edellisenTilanAlkupäivä = modelData(items[0], 'alku') && new Date(modelData(items[0], 'alku'))

  return (
      <div>
        <ul className="array">
          {
            items.map((item, i) => {
              return (<li key={i}>
                <OpiskeluoikeusjaksoEditor model={item} />
                {wrappedModel.context.edit && i === 0 && items.length > 1 && <a className="remove-item" onClick={removeItem}></a>}
              </li>)
            })
          }
          {
            showLisaaTila && <li className="add-item"><a onClick={showAddDialog}>Lisää opiskeluoikeuden tila</a></li>
          }
        </ul>
        {
          addingNew.map(adding => adding && <OpiskeluoikeudenUusiTilaPopup tilaListModel={jaksotModel} suorituksiaKesken={suorituksiaKesken} edellisenTilanAlkupäivä={edellisenTilanAlkupäivä} resultCallback={(uusiJakso) => lisääJakso(uusiJakso)} />)
        }
      </div>
  )
}

export const onLopputila = (tila) => {
  let koodi = modelData(tila).koodiarvo
  return koodi === 'eronnut' || koodi === 'valmistunut' || koodi === 'katsotaaneronneeksi'
}

export const onLopputilassa = (opiskeluoikeus) => {
  let jakso = viimeinenJakso(opiskeluoikeus)
  if (!jakso) return false
  return onLopputila(modelLookup(jakso, 'tila'))
}

const setActive = (jaksot) => {
  let today = new Date()
  let active = jaksot.findIndex(j => parseISODate(modelData(j, 'alku')) < today)
  jaksot[active] = R.assoc('active', true, jaksot[active])
  return jaksot
}

const viimeinenJakso = (opiskeluoikeus) => modelItems(opiskeluoikeusjaksot(opiskeluoikeus)).last()

const opiskeluoikeusjaksot = (opiskeluoikeus) => {
  return modelLookup(opiskeluoikeus, 'tila.opiskeluoikeusjaksot')
}

let fixPäättymispäivä = (opiskeluoikeus) => {
  let päättymispäivä = onLopputilassa(opiskeluoikeus)
    ? modelLookup(viimeinenJakso(opiskeluoikeus), 'alku').value
    : null

  return modelSetValue(opiskeluoikeus, päättymispäivä, 'päättymispäivä')
}