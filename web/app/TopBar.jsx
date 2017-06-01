import React from 'baret'
import {UserInfo} from './UserInfo.jsx'
import Link from './Link.jsx'
import Text from './Text.jsx'
import {editAtom, startEdit, hasEditAccess} from './i18n-edit'

export const TopBar = ({user, saved, titleKey}) => {
  let onClick = e => {
    startEdit()
    e.stopPropagation()
    e.preventDefault()
  }
  let showEdit = hasEditAccess.and(editAtom.not())

  return (<header id='topbar' className={saved ? 'saved' : ''}>
      <div id='logo'><Text name="Opintopolku.fi"/></div>
      <h1><Link href="/koski/"><Text name="Koski"/></Link>{titleKey ?
          <span>{' - '}<Text name={titleKey}/></span> : ''}</h1>{showEdit.map((show) => show && <a className="edit-localizations" onClick={onClick}>{""}</a>)}
      <span className="save-info"><Text name="Kaikki muutokset tallennettu."/></span>
      <UserInfo user={user}/>
    </header>
  )
}