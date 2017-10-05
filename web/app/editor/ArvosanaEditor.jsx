import React from 'baret'
import {Editor} from './Editor.jsx'
import {wrapOptional} from './OptionalEditor.jsx'
import * as L from 'partial.lenses'
import {lensedModel, modelData, modelLookup, modelSetValue, oneOfPrototypes} from './EditorModel'
import {sortGrades} from '../sorting'
import {completeWithFieldAlternatives} from './PerusopetuksenOppiaineRowEditor.jsx'
import {fixArviointi} from './Suoritus'

export const ArvosanaEditor = ({model}) => {
  model = fixArviointi(model)
  let alternativesP = completeWithFieldAlternatives(oneOfPrototypes(wrapOptional({model: modelLookup(model, 'arviointi.-1')})), 'arvosana').startWith([])
  let arvosanatP = alternativesP.map(alternatives => alternatives.map(m => modelLookup(m, 'arvosana').value))
  return (<span>{
    alternativesP.map(alternatives => {
      let arvosanaLens = L.lens(
        (m) => {
          return modelLookup(m, '-1.arvosana')
        },
        (v, m) => {
          if (modelData(v)) {
            // Arvosana valittu -> valitaan vastaava prototyyppi (eri prototyypit eri arvosanoille)
            let valittuKoodiarvo = modelData(v).koodiarvo
            let found = alternatives.find(alt => {
              return modelData(alt, 'arvosana').koodiarvo == valittuKoodiarvo
            })
            return modelSetValue(m, found.value, '-1')
          } else {
            // Ei arvosanaa -> poistetaan arviointi kokonaan
            return modelSetValue(m, undefined)
          }
        }
      )
      let arviointiModel = modelLookup(model, 'arviointi')
      let arvosanaModel = lensedModel(arviointiModel, arvosanaLens)
      // Use key to ensure re-render when alternatives are supplied
      return <Editor key={alternatives.length} model={ arvosanaModel } sortBy={sortGrades} fetchAlternatives={() => arvosanatP} showEmptyOption="true"/>
    })
  }</span>)
}