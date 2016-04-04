import React from 'react'
import Bacon from 'baconjs'
import R from 'ramda'
import Http from './http'
import Dropdown from './Dropdown.jsx'

export const opiskeluOikeusChange = Bacon.Bus()

export const OpiskeluOikeus = React.createClass({
  render() {
    let {opiskeluOikeus} = this.props
    let {rakenne} = this.state
    return (
        rakenne
          ? <div className="opiskeluoikeus">
              <span className="tutkinto">{opiskeluOikeus.suoritus.koulutusmoduuli.tunniste.nimi}</span> <span className="oppilaitos">{opiskeluOikeus.oppilaitos.nimi}</span>
              <div className="tutkinto-rakenne">
                <Dropdown className="suoritustapa"
                          title="Suoritustapa"
                          options={rakenne.suoritustavat.map(s => s.suoritustapa)}
                          value={opiskeluOikeus.suoritus.suoritustapa ? opiskeluOikeus.suoritus.suoritustapa.tunniste.koodiarvo : ''}
                          autoselect={true}
                          onChange={(value) => opiskeluOikeusChange.push([opiskeluOikeus.id,
                            oo => {
                              let suoritustapa = value ? {
                                tunniste: {
                                  koodiarvo: value,
                                  koodistoUri: 'suoritustapa'
                                }
                              } : undefined
                              return oo.mergeDeep({suoritus: {suoritustapa: suoritustapa}})
                            }
                          ])}
                />
                <Dropdown className="osaamisala"
                          title="Osaamisala"
                          options={rakenne.osaamisalat}
                          value={opiskeluOikeus.suoritus.osaamisala ? opiskeluOikeus.suoritus.osaamisala[0].koodiarvo : ''}
                          onChange={(value) => opiskeluOikeusChange.push([opiskeluOikeus.id,
                            oo => {
                              let osaamisala = value ? [{
                                  koodiarvo: value,
                                  koodistoUri: 'osaamisala'
                              }] : undefined
                              return oo.mergeDeep({suoritus: {osaamisala: osaamisala}})
                            }
                          ])}
                  />
                { opiskeluOikeus.suoritus.suoritustapa
                  ? rakenne.suoritustavat.find(x => x.suoritustapa.koodiarvo == opiskeluOikeus.suoritus.suoritustapa.tunniste.koodiarvo).rakenne.osat.map(rakenneOsa => <Rakenneosa
                      rakenneosa={rakenneOsa}
                      opiskeluOikeus={opiskeluOikeus}
                      rakenne={rakenne}
                      key={rakenneOsa.nimi}
                    />)
                  : null
                }
              </div>
            </div>
          : null
    )
  },
  componentDidMount() {
    let {opiskeluOikeus} = this.props
    let diaarinumero = opiskeluOikeus.suoritus.koulutusmoduuli.perusteenDiaarinumero
    Http.get('/tor/api/tutkinto/rakenne/' + encodeURIComponent(diaarinumero)).onValue(rakenne => {
        if (this.isMounted()) {
          this.setState({rakenne: rakenne})
        }
      }
    )
  },
  getInitialState() {
    return {}
  }
})

const Rakenneosa = React.createClass({
  render() {
    let { rakenneosa, opiskeluOikeus, rakenne } = this.props
    return rakenneosa.osat
      ? <RakenneModuuli key={rakenneosa.nimi} opiskeluOikeus={opiskeluOikeus} rakenneosa={rakenneosa} rakenne={rakenne}/>
      : <TutkinnonOsa key={rakenneosa.nimi} opiskeluOikeus={opiskeluOikeus} tutkinnonOsa={rakenneosa} rakenne={rakenne}/>
  }
})

const RakenneModuuli = React.createClass({
  render() {
    let { rakenneosa, opiskeluOikeus, rakenne } = this.props
    return (
      <div className="rakenne-moduuli">
        <span className="name">{rakenneosa.nimi}</span>
        <ul className="osat">
          { rakenneosa.osat
            .filter(osa => {
              let osaamisala = opiskeluOikeus.suoritus.osaamisala ? opiskeluOikeus.suoritus.osaamisala[0].koodiarvo : undefined
              return !osa.osaamisalaKoodi || osa.osaamisalaKoodi == osaamisala
            })
            .map((osa, i) => <li key={i}>
              <Rakenneosa
                rakenneosa={osa}
                opiskeluOikeus={opiskeluOikeus}
                rakenne={rakenne}
              />
            </li>)
          }
        </ul>
      </div>
    )
  }
})

const TutkinnonOsa = React.createClass({
  render() {
    const {tutkinnonOsa, opiskeluOikeus, rakenne} = this.props
    const arviointiAsteikko = R.find(asteikko => R.equals(asteikko.koodisto, tutkinnonOsa.arviointiAsteikko))(rakenne.arviointiAsteikot)
    const arvosanat = arviointiAsteikko ? arviointiAsteikko.arvosanat : undefined
    const laajuudenYksikkö = R.find(tapa => {
      return tapa.suoritustapa.koodiarvo == opiskeluOikeus.suoritus.suoritustapa.tunniste.koodiarvo
    })(rakenne.suoritustavat).laajuusYksikkö

    const addArvosana = (arvosana) => (oOikeus) => {
      let oo = oOikeus.hasIn['suoritus','osasuoritukset'] ? oOikeus : oOikeus.mergeDeep({suoritus: {osasuoritukset: []}})

      return oo.updateIn(['suoritus', 'osasuoritukset'], x => x.push(
        {
          koulutusmoduuli: {
            tunniste: tutkinnonOsa.tunniste,
            pakollinen: tutkinnonOsa.pakollinen,
            laajuus: tutkinnonOsa.laajuus ? {
              arvo : tutkinnonOsa.laajuus,
              yksikkö : laajuudenYksikkö
            } : undefined
          },
          tila: { koodistoUri: 'suorituksentila', koodiarvo: 'KESKEN' },
          arviointi: [
            {
              arvosana: arvosana
            }
          ],
          toimipiste: oo.getIn(['suoritus','toimipiste']),
          tyyppi: { 'koodistoUri': 'suorituksentyyppi', 'koodiarvo': 'ammatillinenopstutkinnonosasuoritus'}
        }
      ))
    }

    const saveArvosana = (arvosana) => {
      opiskeluOikeusChange.push([opiskeluOikeus.id, addArvosana(arvosana)])
    }

    const suoritus = R.find(osanSuoritus => R.equals(osanSuoritus.koulutusmoduuli.tunniste, tutkinnonOsa.tunniste))(opiskeluOikeus.suoritus.osasuoritukset ? opiskeluOikeus.suoritus.osasuoritukset : [])
    const arviointi = suoritus && suoritus.arviointi

    return (
      <div className={ arviointi ? 'tutkinnon-osa suoritettu' : 'tutkinnon-osa'}>
        <span className="name">{tutkinnonOsa.nimi}</span>
        { arvosanat && !arviointi
          ?
            <div className="arviointi edit">
              <ul className="arvosanat">{
                arvosanat.map((arvosana) => <li className= { arvosana == this.state.valittuArvosana ? 'selected' : '' } key={arvosana.koodiarvo} onClick={() => this.setState({ valittuArvosana: arvosana })}>{arvosana.nimi}</li>)
              }</ul>
              <button className="button blue" disabled={!this.state.valittuArvosana} onClick={() => saveArvosana(this.state.valittuArvosana)}>Tallenna arvio</button>
            </div>
          : (
            arviointi
              ? <div className="arviointi"><span className="arvosana">{arviointi[arviointi.length - 1].arvosana.nimi}</span></div>
              : null
          )
        }
      </div>
    )
  },
  getInitialState() {
    return {}
  }
})